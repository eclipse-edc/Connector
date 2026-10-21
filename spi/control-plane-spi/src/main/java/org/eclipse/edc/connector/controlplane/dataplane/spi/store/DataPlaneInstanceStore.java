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
 * <p>
 * The identity of a {@link DataPlaneInstance} is the pair (participant context id, id): the same id can be used by
 * different participant contexts.
 */
public interface DataPlaneInstanceStore {


    /**
     * Finds the entity for the participant context and id or null.
     *
     * @param participantContextId the participant context id.
     * @param id the id.
     * @return the entity if found, null otherwise.
     */
    @Nullable
    DataPlaneInstance findById(String participantContextId, String id);

    /**
     * Persists the entity. This follows UPSERT semantics within the participant context of the entity, so if the object
     * didn't exit before, it's created.
     *
     * @param entity the entity.
     */
    StoreResult<Void> save(DataPlaneInstance entity);

    /**
     * Delete a data plane instance by its participant context and id.
     *
     * @param participantContextId the participant context id.
     * @param instanceId the data plane instance id.
     * @return the deleted data plane instance.
     */
    StoreResult<DataPlaneInstance> deleteById(String participantContextId, String instanceId);

    Stream<DataPlaneInstance> query(QuerySpec querySpec);


}
