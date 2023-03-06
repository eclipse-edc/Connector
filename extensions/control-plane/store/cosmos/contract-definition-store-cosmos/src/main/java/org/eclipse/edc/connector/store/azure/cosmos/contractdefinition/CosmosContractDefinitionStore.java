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
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;

import com.azure.cosmos.implementation.NotFoundException;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.dialect.SqlStatement;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.model.ContractDefinitionDocument;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;

/**
 * Implementation of the {@link ContractDefinitionStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) hit the cache, while write operations affect both the cache AND the
 * database.
 */
public class CosmosContractDefinitionStore implements ContractDefinitionStore {
    private final CosmosDbApi cosmosDbApi;
    private final TypeManager typeManager;
    private final RetryPolicy<Object> retryPolicy;
    private final String partitionKey;
    private final Monitor monitor;

    public CosmosContractDefinitionStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, RetryPolicy<Object> retryPolicy, String partitionKey, Monitor monitor) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.partitionKey = partitionKey;
        this.monitor = monitor;
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        var statement = new SqlStatement<>(ContractDefinitionDocument.class);
        var query = statement.where(spec.getFilterExpression())
                .offset(spec.getOffset())
                .limit(spec.getLimit())
                .orderBy(spec.getSortField(), spec.getSortOrder() == SortOrder.ASC)
                .getQueryAsSqlQuerySpec();

        var objects = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query));
        return objects.map(this::convert);
    }

    @Override
    public ContractDefinition findById(String definitionId) {
        var definition = with(retryPolicy).get(() -> cosmosDbApi.queryItemById(definitionId));
        return definition != null ? convert(definition) : null;
    }

    @Override
    public void save(Collection<ContractDefinition> definitions) {
        with(retryPolicy).run(() -> cosmosDbApi.createItems(definitions.stream().map(this::convertToDocument).collect(Collectors.toList())));
    }

    @Override
    public void save(ContractDefinition definition) {
        with(retryPolicy).run(() -> cosmosDbApi.createItem(convertToDocument(definition)));
    }

    @Override
    public void update(ContractDefinition definition) {
        save(definition); //cosmos db api internally uses "upsert" semantics
    }

    @Override
    public ContractDefinition deleteById(String id) {
        try {
            var deletedItem = with(retryPolicy).get(() -> cosmosDbApi.deleteItem(id));
            return deletedItem == null ? null : convert(deletedItem);
        } catch (NotFoundException e) {
            monitor.debug(() -> String.format("ContractDefinition with id %s not found", id));
            return null;
        }
    }

    @Override
    public void reload() {
    }

    @NotNull
    private ContractDefinitionDocument convertToDocument(ContractDefinition def) {
        return new ContractDefinitionDocument(def, partitionKey);
    }


    private ContractDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractDefinitionDocument.class).getWrappedInstance();
    }
}
