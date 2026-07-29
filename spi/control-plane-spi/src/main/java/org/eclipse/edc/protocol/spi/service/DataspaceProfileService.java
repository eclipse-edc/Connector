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

package org.eclipse.edc.protocol.spi.service;

import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Manages the lifecycle of {@link DataspaceProfile}s. Beyond persistence, mutations are kept in sync
 * with the {@link org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry} so they take effect on
 * the running connector immediately: {@link #create(DataspaceProfile)} and {@link #update(DataspaceProfile)}
 * (re-)register the profile, while {@link #deleteById(String)} deregisters it.
 */
@ExtensionPoint
public interface DataspaceProfileService {

    /**
     * Returns a profile by its name.
     *
     * @param name the name of the profile.
     * @return the profile, or null if not found.
     */
    DataspaceProfile findById(String name);

    /**
     * Search profiles.
     *
     * @param query the query spec.
     * @return the list of profiles matching the query.
     */
    ServiceResult<List<DataspaceProfile>> search(QuerySpec query);

    /**
     * Creates a profile, persisting it and registering it into the {@code DataspaceProfileContextRegistry}.
     *
     * @param profile the profile to create.
     * @return success with the created profile, a failure (e.g. conflict) otherwise.
     */
    @NotNull
    ServiceResult<DataspaceProfile> create(DataspaceProfile profile);

    /**
     * Updates an existing profile, persisting the change and re-registering it into the
     * {@code DataspaceProfileContextRegistry} so it takes effect on the running connector.
     *
     * @param profile the profile to update.
     * @return success with the updated profile, a failure (e.g. not found) otherwise.
     */
    @NotNull
    ServiceResult<DataspaceProfile> update(DataspaceProfile profile);

    /**
     * Deletes a profile from the store and deregisters it from the {@code DataspaceProfileContextRegistry}.
     *
     * @param name the name of the profile to delete.
     * @return success with the deleted profile, a failure (e.g. not found) otherwise.
     */
    @NotNull
    ServiceResult<DataspaceProfile> deleteById(String name);
}
