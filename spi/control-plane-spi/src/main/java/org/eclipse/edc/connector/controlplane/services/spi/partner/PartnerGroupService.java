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

package org.eclipse.edc.connector.controlplane.services.spi.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service layer for {@link PartnerGroup}s. All operations are scoped to a participant context.
 */
public interface PartnerGroupService {

    /**
     * Finds a group by id within a participant context.
     *
     * @return the group, or a not-found failure.
     */
    ServiceResult<PartnerGroup> findById(String participantContextId, String id);

    /**
     * Searches groups. The query must carry a participant context filter.
     *
     * @return the matching groups, or a bad-request failure if the query is invalid.
     */
    ServiceResult<List<PartnerGroup>> search(QuerySpec query);

    /**
     * Creates a group.
     *
     * @return the created group, or a conflict failure if the id is already taken.
     */
    ServiceResult<PartnerGroup> create(PartnerGroup group);

    /**
     * Updates a group.
     *
     * @return the updated group, or a not-found failure.
     */
    ServiceResult<PartnerGroup> update(PartnerGroup group);

    /**
     * Deletes a group. A group that is still referenced by a partner cannot be deleted.
     *
     * @return the deleted group, a not-found failure, or a conflict failure if partners still reference it.
     */
    ServiceResult<PartnerGroup> delete(String participantContextId, String id);

}
