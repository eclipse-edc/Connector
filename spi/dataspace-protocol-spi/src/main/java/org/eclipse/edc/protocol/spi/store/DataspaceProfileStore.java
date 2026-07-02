/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi.store;

import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.stream.Stream;

/**
 * Persists {@link DataspaceProfile}.
 */
@ExtensionPoint
public interface DataspaceProfileStore {

    String PROFILE_NOT_FOUND = "Dataspace profile with name %s could not be found";
    String PROFILE_ALREADY_EXISTS = "Dataspace profile with name %s already exists";

    /**
     * Finds a profile by its name.
     *
     * @param name the name of the profile.
     * @return the {@link DataspaceProfile} or null if not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    DataspaceProfile findById(String name);

    /**
     * Find the stream of profiles in the store based on the query spec.
     *
     * @param spec query specification.
     * @return a {@link Stream} of {@link DataspaceProfile}. Might be empty, never null.
     * @throws EdcPersistenceException if something goes wrong.
     */
    Stream<DataspaceProfile> findAll(QuerySpec spec);

    /**
     * Persists the profile, if it does not yet exist.
     *
     * @param profile to be saved.
     * @return {@link StoreResult#success()} if it could be stored, {@link StoreResult#alreadyExists(String)} if a profile with the same name already exists.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<DataspaceProfile> create(DataspaceProfile profile);

    /**
     * Updates the profile.
     *
     * @param profile to be updated.
     * @return {@link StoreResult#success()} if it could be updated, {@link StoreResult#notFound(String)} if a profile with the same name was not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<DataspaceProfile> update(DataspaceProfile profile);

    /**
     * Deletes the profile for the given name.
     *
     * @param name name of the profile to be removed.
     * @return {@link StoreResult#success()} if it was deleted, {@link StoreResult#notFound(String)} if a profile with the same name was not found.
     * @throws EdcPersistenceException if something goes wrong.
     */
    StoreResult<DataspaceProfile> delete(String name);
}
