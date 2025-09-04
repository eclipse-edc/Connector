/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.defaults.store;

import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryParticipantContextStore implements ParticipantContextStore {

    private final QueryResolver<ParticipantContext> participantContextQueryResolver;
    private final Map<String, ParticipantContext> participants = new ConcurrentHashMap<>();

    public InMemoryParticipantContextStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.participantContextQueryResolver = new ReflectionBasedQueryResolver<>(ParticipantContext.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Collection<ParticipantContext>> query(QuerySpec querySpec) {
        return StoreResult.success(participantContextQueryResolver.query(participants.values().stream(), querySpec)
                .collect(Collectors.toList()));
    }

    @Override
    public StoreResult<Void> create(ParticipantContext participantContext) {
        var prev = participants.putIfAbsent(participantContext.getParticipantContextId(), participantContext);
        if (prev != null) {
            return StoreResult.alreadyExists("ParticipantContext with ID '%s' already exists.".formatted(participantContext.getParticipantContextId()));
        }
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> update(ParticipantContext participantContext) {
        var prev = participants.replace(participantContext.getParticipantContextId(), participantContext);
        if (prev != null) {
            return StoreResult.success();
        } else {
            return StoreResult.notFound("ParticipantContext with ID '%s' not found.".formatted(participantContext.getParticipantContextId()));
        }
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        var prev = participants.remove(id);
        if (prev != null) {
            return StoreResult.success();
        } else {
            return StoreResult.notFound("ParticipantContext with ID '%s' not found.".formatted(id));
        }
    }
}
