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
 * Manages the lifecycle of {@link DataspaceProfile}s. Beyond persistence, a successful
 * {@link #create(DataspaceProfile)} also registers the profile into the
 * {@link org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry} so it takes effect on the
 * running connector.
 * <p>
 * Note: the registry is append-only, therefore {@link #deleteById(String)} only removes the profile
 * from the store; the running registry keeps the entry until the next boot.
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
     * Deletes a profile from the store. Does not affect the running registry (which is append-only);
     * the change takes effect on the next boot.
     *
     * @param name the name of the profile to delete.
     * @return success with the deleted profile, a failure (e.g. not found) otherwise.
     */
    @NotNull
    ServiceResult<DataspaceProfile> deleteById(String name);
}
