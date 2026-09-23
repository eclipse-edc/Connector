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

package org.eclipse.edc.connector.controlplane.partner.spi.store;

import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Persists {@link PartnerGroup}s. A group is identified by the pair (participant context id, id).
 */
@ExtensionPoint
public interface PartnerGroupStore {

    String PARTNER_GROUP_NOT_FOUND = "PartnerGroup with ID %s could not be found in participant context %s";
    String PARTNER_GROUP_ALREADY_EXISTS = "PartnerGroup with ID %s already exists in participant context %s";

    /**
     * Finds a group by id within a participant context.
     *
     * @param participantContextId the owning participant context.
     * @param id                   the group id.
     * @return the group or null if it does not exist in the participant context.
     */
    @Nullable
    PartnerGroup findById(String participantContextId, String id);

    /**
     * Queries groups. Callers are expected to add a participant context filter.
     *
     * @param querySpec the query.
     * @return the matching groups, never null.
     */
    List<PartnerGroup> query(QuerySpec querySpec);

    /**
     * Persists a group.
     *
     * @return success, or {@link StoreResult#alreadyExists(String)} if a group with the same id exists in the
     *         participant context.
     */
    StoreResult<Void> create(PartnerGroup group);

    /**
     * Updates a group.
     *
     * @return success, or {@link StoreResult#notFound(String)} if the group does not exist in the participant context.
     */
    StoreResult<Void> update(PartnerGroup group);

    /**
     * Deletes a group.
     *
     * @return success, or {@link StoreResult#notFound(String)} if it does not exist in the participant context.
     */
    StoreResult<Void> deleteById(String participantContextId, String id);

}
