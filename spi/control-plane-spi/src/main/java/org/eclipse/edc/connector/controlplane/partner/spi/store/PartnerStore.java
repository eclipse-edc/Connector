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

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Persists {@link Partner}s. A partner is identified by the pair (participant context id, id); the same id may exist
 * in different participant contexts. Within a participant context the partner identity is unique as well.
 */
@ExtensionPoint
public interface PartnerStore {

    String PARTNER_NOT_FOUND = "Partner with ID %s could not be found in participant context %s";
    String PARTNER_ALREADY_EXISTS = "Partner with ID %s already exists in participant context %s";
    String PARTNER_IDENTITY_ALREADY_EXISTS = "A Partner with identity %s already exists in participant context %s";

    /**
     * Finds a partner by id within a participant context.
     *
     * @param participantContextId the owning participant context.
     * @param id                   the partner id.
     * @return the partner or null if it does not exist in the participant context.
     */
    @Nullable
    Partner findById(String participantContextId, String id);

    /**
     * Finds a partner by its counterparty identity within a participant context.
     *
     * @param participantContextId the owning participant context.
     * @param identity             the counterparty identity.
     * @return the partner or null if none has that identity in the participant context.
     */
    @Nullable
    default Partner findByIdentity(String participantContextId, String identity) {
        var query = queryByParticipantContextId(participantContextId)
                .filter(criterion("identity", "=", identity))
                .limit(1)
                .build();
        return query(query).stream().findFirst().orElse(null);
    }

    /**
     * Queries partners. Callers are expected to add a participant context filter.
     *
     * @param querySpec the query.
     * @return the matching partners, never null.
     */
    List<Partner> query(QuerySpec querySpec);

    /**
     * Persists a partner.
     *
     * @return success, or {@link StoreResult#alreadyExists(String)} if a partner with the same id or identity exists
     *         in the participant context.
     */
    StoreResult<Void> create(Partner partner);

    /**
     * Updates a partner.
     *
     * @return success, or {@link StoreResult#notFound(String)} if the partner does not exist in the participant
     *         context, or {@link StoreResult#alreadyExists(String)} if the new identity is taken by another partner.
     */
    StoreResult<Void> update(Partner partner);

    /**
     * Deletes a partner.
     *
     * @return success, or {@link StoreResult#notFound(String)} if it does not exist in the participant context.
     */
    StoreResult<Void> deleteById(String participantContextId, String id);

}
