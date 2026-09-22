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

import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * In-memory {@link PartnerGroupStore}. Entries are keyed by (participant context id, id).
 */
public class InMemoryPartnerGroupStore implements PartnerGroupStore {

    private final Map<String, PartnerGroup> groups = new ConcurrentHashMap<>();
    private final QueryResolver<PartnerGroup> queryResolver;

    public InMemoryPartnerGroupStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(PartnerGroup.class, criterionOperatorRegistry);
    }

    @Override
    public PartnerGroup findById(String participantContextId, String id) {
        return groups.get(key(participantContextId, id));
    }

    @Override
    public List<PartnerGroup> query(QuerySpec querySpec) {
        return queryResolver.query(groups.values().stream(), querySpec).toList();
    }

    @Override
    public StoreResult<Void> create(PartnerGroup group) {
        var previous = groups.putIfAbsent(key(group.getParticipantContextId(), group.getId()), group);
        return previous == null
                ? StoreResult.success()
                : StoreResult.alreadyExists(format(PARTNER_GROUP_ALREADY_EXISTS, group.getId(), group.getParticipantContextId()));
    }

    @Override
    public StoreResult<Void> update(PartnerGroup group) {
        var previous = groups.replace(key(group.getParticipantContextId(), group.getId()), group);
        return previous == null
                ? StoreResult.notFound(format(PARTNER_GROUP_NOT_FOUND, group.getId(), group.getParticipantContextId()))
                : StoreResult.success();
    }

    @Override
    public StoreResult<Void> deleteById(String participantContextId, String id) {
        var removed = groups.remove(key(participantContextId, id));
        return removed == null
                ? StoreResult.notFound(format(PARTNER_GROUP_NOT_FOUND, id, participantContextId))
                : StoreResult.success();
    }

    private String key(String participantContextId, String id) {
        return participantContextId + ":" + id;
    }
}
