/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.BadRequestException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedRunnable;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.dialect.SqlStatement;
import org.eclipse.edc.azure.cosmos.util.CosmosLeaseContext;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.model.ContractNegotiationDocument;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Implementation of the {@link ContractDefinitionStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) hit the cache, while write operations affect both the cache AND the
 * database.
 */
public class CosmosContractNegotiationStore implements ContractNegotiationStore {
    private static final String NEXT_FOR_STATE_SPROC_NAME = "nextForState";
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final String connectorId;
    private final String partitionKey;
    private final CosmosLeaseContext leaseContext;
    private final Clock clock;

    public CosmosContractNegotiationStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String connectorId, Clock clock) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.connectorId = connectorId;
        partitionKey = connectorId;
        this.clock = clock;
        leaseContext = CosmosLeaseContext.with(cosmosDbApi, partitionKey, connectorId).usingRetry(List.of(retryPolicy));
    }

    @Override
    public @Nullable ContractNegotiation findById(String negotiationId) {
        var object = with(retryPolicy).get(() -> findByIdInternal(negotiationId));
        return object != null ? toNegotiation(object) : null;
    }

    @Override
    public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
        final String query = "SELECT * FROM c WHERE (c.wrappedInstance.correlationId = @corrId)";
        SqlParameter param = new SqlParameter("@corrId", correlationId);
        var querySpec = new SqlQuerySpec(query, param);

        //todo: throw exception if more than 1 element?
        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(querySpec));
        return objects.findFirst().map(this::toNegotiation).orElse(null);
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        final String query = "SELECT * FROM c WHERE c.wrappedInstance.contractAgreement.id = @contractId";
        SqlParameter param = new SqlParameter("@contractId", contractId);

        var spec = new SqlQuerySpec(query, param);
        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(spec));
        return objects.findFirst().map(o -> toNegotiation(o).getContractAgreement()).orElse(null);
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        try {
            leaseContext.acquireLease(negotiation.getId());
            CheckedRunnable action;
            if (findByIdInternal(negotiation.getId()) != null) {
                action = () -> cosmosDbApi.updateItem(new ContractNegotiationDocument(negotiation, partitionKey));
            } else {
                action = () -> cosmosDbApi.createItem(new ContractNegotiationDocument(negotiation, partitionKey));
            }
            with(retryPolicy).run(action);
            leaseContext.breakLease(negotiation.getId());
        } catch (BadRequestException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void delete(String negotiationId) {
        try {
            var item = findByIdInternal(negotiationId);
            if (item == null) {
                return; //nothing to do
            }
            var document = toNegotiationDocument(item);
            if (document.getWrappedInstance().getContractAgreement() != null) {
                throw new IllegalStateException(format("Cannot delete ContractNegotiation [%s]: ContractAgreement already created.", negotiationId));
            }
            var lease = document.getLease();
            if (lease != null && !lease.isExpired(clock.millis())) {
                throw new IllegalStateException(format("The ContractNegotiation [%s] cannot be deleted: it is currently leased", negotiationId));
            }

            leaseContext.acquireLease(negotiationId);
            with(retryPolicy).run(() -> cosmosDbApi.deleteItem(negotiationId));
            leaseContext.breakLease(negotiationId);

        } catch (NotFoundException ignored) {
            // noop here
        } catch (CosmosException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        var statement = new SqlStatement<>(ContractNegotiationDocument.class);
        var query = statement.where(querySpec.getFilterExpression())
                .offset(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .orderBy(querySpec.getSortField(), querySpec.getSortOrder() == SortOrder.ASC)
                .getQueryAsSqlQuerySpec();

        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query));
        return objects.map(this::toNegotiation);
    }

    @Override
    public @NotNull Stream<ContractAgreement> queryAgreements(QuerySpec querySpec) {
        var criteria = querySpec.getFilterExpression().stream()
                .map(it -> it.withLeftOperand(op -> "contractAgreement." + op))
                .collect(Collectors.toList());

        var sortField = ofNullable(querySpec.getSortField()).map(it -> "contractAgreement." + it).orElse(null);

        var query = new SqlStatement<>(ContractNegotiationDocument.class)
                .where(criteria)
                .offset(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .orderBy(sortField, querySpec.getSortOrder() == SortOrder.ASC)
                .getQueryAsSqlQuerySpec();

        return with(retryPolicy).get(() -> cosmosDbApi.queryItems(query))
                .map(this::toNegotiation)
                .map(ContractNegotiation::getContractAgreement)
                .filter(Objects::nonNull);
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {

        String rawJson = with(retryPolicy).get(() -> cosmosDbApi.invokeStoredProcedure(NEXT_FOR_STATE_SPROC_NAME, partitionKey, state, max, connectorId));
        if (StringUtils.isNullOrEmpty(rawJson)) {
            return Collections.emptyList();
        }

        var typeRef = new TypeReference<List<Object>>() {
        };
        var list = typeManager.readValue(rawJson, typeRef);
        return list.stream().map(this::toNegotiation).collect(Collectors.toList());
    }

    @Nullable
    private Object findByIdInternal(String negotiationId) {
        return cosmosDbApi.queryItemById(negotiationId);
    }

    private ContractNegotiation toNegotiation(Object object) {
        return toNegotiationDocument(object).getWrappedInstance();
    }

    private ContractNegotiationDocument toNegotiationDocument(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractNegotiationDocument.class);
    }
}

