/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.spi.store;

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

/**
 * Persistence layer for {@link AccessTokenData} objects, which the data plane uses to keep track of all access tokens that
 * are currently valid.
 * Implementations must be thread-safe.
 */
public interface AccessTokenDataStore {
    String OBJECT_EXISTS = "AccessTokenData with ID '%s' already exists.";
    String OBJECT_NOT_FOUND = "AccessTokenData with ID '%s' does not exist.";

    /**
     * Returns an {@link AccessTokenData} object with the given ID. Returns null if not found.
     *
     * @param id the ID of the entity.
     * @return The entity, or null if not found.
     * @throws NullPointerException if the id parameter was null
     */
    AccessTokenData getById(String id);

    /**
     * Adds an {@link AccessTokenData} object to the persistence layer. Will return a failure if an object with the same already exists.
     *
     * @param accessTokenData the new object
     * @return success if stored, a failure if an object with the same ID already exists.
     */
    StoreResult<Void> store(AccessTokenData accessTokenData);

    /**
     * Deletes an {@link AccessTokenData} entity with the given ID.
     *
     * @param id The ID of the {@link AccessTokenData} that is supposed to be deleted.
     * @return success if deleted, a failure if an object with the given ID does not exist.
     */
    StoreResult<AccessTokenData> deleteById(String id);

    /**
     * Returns all {@link AccessTokenData} objects in the store that are covered by a given {@link QuerySpec}.
     * <p>
     * Note: supplying a sort field that does not exist on the {@link AccessTokenData} may cause some implementations
     * to return an empty Stream, others will return an unsorted Stream, depending on the backing storage
     * implementation.
     */
    Collection<AccessTokenData> query(QuerySpec querySpec);

}
