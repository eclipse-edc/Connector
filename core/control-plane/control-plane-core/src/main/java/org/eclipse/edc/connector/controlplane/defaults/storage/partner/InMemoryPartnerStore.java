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

package org.eclipse.edc.connector.controlplane.defaults.storage.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * In-memory {@link PartnerStore}. Entries are keyed by (participant context id, id).
 */
public class InMemoryPartnerStore implements PartnerStore {

    private final Map<String, Partner> partners = new ConcurrentHashMap<>();
    private final QueryResolver<Partner> queryResolver;

    public InMemoryPartnerStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(Partner.class, criterionOperatorRegistry);
    }

    @Override
    public Partner findById(String participantContextId, String id) {
        return partners.get(key(participantContextId, id));
    }

    @Override
    public List<Partner> query(QuerySpec querySpec) {
        return queryResolver.query(partners.values().stream(), querySpec).toList();
    }

    @Override
    public synchronized StoreResult<Void> create(Partner partner) {
        var key = key(partner.getParticipantContextId(), partner.getId());
        if (partners.containsKey(key)) {
            return StoreResult.alreadyExists(format(PARTNER_ALREADY_EXISTS, partner.getId(), partner.getParticipantContextId()));
        }
        if (identityTaken(partner)) {
            return StoreResult.alreadyExists(format(PARTNER_IDENTITY_ALREADY_EXISTS, partner.getIdentity(), partner.getParticipantContextId()));
        }
        partners.put(key, partner);
        return StoreResult.success();
    }

    @Override
    public synchronized StoreResult<Void> update(Partner partner) {
        var key = key(partner.getParticipantContextId(), partner.getId());
        if (!partners.containsKey(key)) {
            return StoreResult.notFound(format(PARTNER_NOT_FOUND, partner.getId(), partner.getParticipantContextId()));
        }
        if (identityTaken(partner)) {
            return StoreResult.alreadyExists(format(PARTNER_IDENTITY_ALREADY_EXISTS, partner.getIdentity(), partner.getParticipantContextId()));
        }
        partners.put(key, partner);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> deleteById(String participantContextId, String id) {
        var removed = partners.remove(key(participantContextId, id));
        return removed == null
                ? StoreResult.notFound(format(PARTNER_NOT_FOUND, id, participantContextId))
                : StoreResult.success();
    }

    private boolean identityTaken(Partner partner) {
        return partners.values().stream()
                .anyMatch(existing -> Objects.equals(existing.getParticipantContextId(), partner.getParticipantContextId()) &&
                        Objects.equals(existing.getIdentity(), partner.getIdentity()) &&
                        !Objects.equals(existing.getId(), partner.getId()));
    }

    private String key(String participantContextId, String id) {
        return participantContextId + ":" + id;
    }
}
