/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.store.cosmos;

import com.azure.cosmos.implementation.ConflictException;
import com.azure.cosmos.implementation.NotFoundException;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.dialect.SqlStatement;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Optional;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;
import static java.lang.String.format;

/**
 * Implementation of the {@link DataPlaneInstanceStore} based on CosmosDB. This store implements simple write-through
 * caching mechanics: read operations (e.g. findAll) always hit the cache, while write operations affect both the cache
 * AND the database.
 */
public class CosmosDataPlaneInstanceStore implements DataPlaneInstanceStore {
    private final CosmosDbApi cosmosDbApi;

    private final TypeManager typeManager;

    private final RetryPolicy<Object> retryPolicy;

    private final String partitionKey;
    private final Monitor monitor;

    public CosmosDataPlaneInstanceStore(CosmosDbApi cosmosDbApi, TypeManager typeManager,
                                        RetryPolicy<Object> retryPolicy, String partitionKey, Monitor monitor) {
        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
        this.partitionKey = partitionKey;
        this.monitor = monitor;
    }

    @Override
    public StoreResult<Void> create(DataPlaneInstance instance) {
        try {
            insertInternal(instance);
            return StoreResult.success();
        } catch (ConflictException exception) {
            var msg = format(DATA_PLANE_INSTANCE_EXISTS, instance.getId());
            monitor.debug(msg);
            return StoreResult.alreadyExists(msg);
        }
    }

    @Override
    public StoreResult<Void> update(DataPlaneInstance instance) {
        try {
            updateInternal(instance);
            return StoreResult.success();
        } catch (NotFoundException exception) {
            var msg = format(DATA_PLANE_INSTANCE_NOT_FOUND, instance.getId());
            monitor.debug(msg);
            return StoreResult.notFound(msg);
        }
    }

    @Override
    public DataPlaneInstance findById(String id) {
        var dataSpaceInstance = with(retryPolicy).get(() -> cosmosDbApi.queryItemById(id));
        return convert(dataSpaceInstance);
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        var statement = new SqlStatement<>(DataPlaneInstanceDocument.class);
        var query = statement.getQueryAsString();
        var dataSpaceInstances = with(retryPolicy).get(() -> cosmosDbApi.queryItems(query));
        return dataSpaceInstances.map(this::convert);
    }

    private void insertInternal(DataPlaneInstance instance) {
        var document = new DataPlaneInstanceDocument(instance, partitionKey);
        with(retryPolicy).run(() -> cosmosDbApi.createItem(document));
    }

    private void updateInternal(DataPlaneInstance instance) {
        var document = new DataPlaneInstanceDocument(instance, partitionKey);
        with(retryPolicy).run(() -> cosmosDbApi.updateItem(document));
    }

    private DataPlaneInstance convert(Object object) {
        var json = Optional.ofNullable(typeManager.writeValueAsString(object));
        return json.map(s -> typeManager.readValue(s, DataPlaneInstanceDocument.class)).map(DataPlaneInstanceDocument::getWrappedInstance).orElse(null);
    }

}
