/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.dataplane.spi.store;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Holds all {@link DataPlaneInstance} objects that are known to the DPF selector.
 * The collection of {@link DataPlaneInstance} objects is mutable at runtime, so implementations must take that into account.
 */
public interface DataPlaneInstanceStore {


    /**
     * Finds the entity for the id or null.
     *
     * @param id the id.
     * @return the entity if found, null otherwise.
     */
    @Nullable
    DataPlaneInstance findById(String id);

    /**
     * Persists the entity. This follows UPSERT semantics, so if the object didn't exit before, it's created.
     *
     * @param entity the entity.
     */
    StoreResult<Void> save(DataPlaneInstance entity);

    /**
     * Delete a data plane instance by its id.
     *
     * @param instanceId the data plane instance id.
     * @return the deleted data plane instance.
     */
    StoreResult<DataPlaneInstance> deleteById(String instanceId);

    Stream<DataPlaneInstance> getAll();

    Stream<DataPlaneInstance> query(QuerySpec querySpec);


}
