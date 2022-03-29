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
 *
 */

package org.eclipse.dataspaceconnector.contract.negotiation.store;

import com.azure.cosmos.implementation.BadRequestException;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.core.type.TypeReference;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.azure.cosmos.dialect.SqlStatement;
import org.eclipse.dataspaceconnector.azure.cosmos.util.CosmosLeaseContext;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.jodah.failsafe.Failsafe.with;

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

    public CosmosContractNegotiationStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String connectorId) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.connectorId = connectorId;
        partitionKey = connectorId;
        leaseContext = CosmosLeaseContext.with(cosmosDbApi, partitionKey, connectorId).usingRetry(List.of(retryPolicy));
    }

    @Override
    public @Nullable ContractNegotiation find(String negotiationId) {
        var object = with(retryPolicy).get(() -> cosmosDbApi.queryItemById(negotiationId));
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
            with(retryPolicy).run(() -> cosmosDbApi.saveItem(new ContractNegotiationDocument(negotiation, partitionKey)));
            leaseContext.breakLease(negotiation.getId());
        } catch (BadRequestException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public void delete(String negotiationId) {
        try {
            leaseContext.acquireLease(negotiationId);
            with(retryPolicy).run(() -> cosmosDbApi.deleteItem(negotiationId));
            leaseContext.breakLease(negotiationId);
        } catch (BadRequestException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        var statement = new SqlStatement<>(ContractNegotiationDocument.class);
        var query = statement.where(querySpec.getFilterExpression()).offset(querySpec.getOffset()).limit(querySpec.getLimit()).orderBy(querySpec.getSortField(), querySpec.getSortOrder() == SortOrder.ASC).getQueryAsSqlQuerySpec();

        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query));
        return objects.map(this::toNegotiation);
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

    private ContractNegotiation toNegotiation(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractNegotiationDocument.class).getWrappedInstance();
    }
}

