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

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;

/**
 * Service layer for {@link Partner}s. All operations are scoped to a participant context. This is the entry point
 * for custom policy functions that want to resolve the counterparty of a policy evaluation to a partner.
 */
public interface PartnerService {

    /**
     * Finds a partner by id within a participant context.
     *
     * @return the partner, or a not-found failure.
     */
    ServiceResult<Partner> findById(String participantContextId, String id);

    /**
     * Finds a partner by counterparty identity within a participant context.
     *
     * @return the partner, or a not-found failure.
     */
    ServiceResult<Partner> findByIdentity(String participantContextId, String identity);

    /**
     * Searches partners. The query must carry a participant context filter.
     *
     * @return the matching partners, or a bad-request failure if the query is invalid.
     */
    ServiceResult<List<Partner>> search(QuerySpec query);

    /**
     * Creates a partner. Every referenced group must exist in the same participant context.
     *
     * @return the created partner, a bad-request failure if a group is unknown, or a conflict failure if the id or
     *         identity is already taken.
     */
    ServiceResult<Partner> create(Partner partner);

    /**
     * Updates a partner. Every referenced group must exist in the same participant context.
     *
     * @return the updated partner, a not-found failure, a bad-request failure if a group is unknown, or a conflict
     *         failure if the identity is taken by another partner.
     */
    ServiceResult<Partner> update(Partner partner);

    /**
     * Deletes a partner.
     *
     * @return the deleted partner, or a not-found failure.
     */
    ServiceResult<Partner> delete(String participantContextId, String id);

}
