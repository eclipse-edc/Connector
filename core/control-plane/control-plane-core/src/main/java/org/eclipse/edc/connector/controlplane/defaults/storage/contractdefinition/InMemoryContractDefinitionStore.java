/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added method
 *
 */

package org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.defaults.storage.ParticipantResourceKey;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * The default store implementation used when no extension is configured in a runtime. {@link ContractDefinition}s are
 * stored ephemerally in memory, keyed by participant context id and definition id.
 */
public class InMemoryContractDefinitionStore implements ContractDefinitionStore {
    private final Map<ParticipantResourceKey, ContractDefinition> cache = new ConcurrentHashMap<>();
    private final QueryResolver<ContractDefinition> queryResolver;

    public InMemoryContractDefinitionStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(ContractDefinition.class, criterionOperatorRegistry);
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return queryResolver.query(cache.values().stream(), spec);
    }

    @Override
    public ContractDefinition findById(String participantContextId, String definitionId) {
        return cache.get(ParticipantResourceKey.of(participantContextId, definitionId));
    }

    @Override
    public StoreResult<Void> save(ContractDefinition definition) {
        var prev = cache.putIfAbsent(ParticipantResourceKey.of(definition.getParticipantContextId(), definition.getId()), definition);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>alreadyExists(format(CONTRACT_DEFINITION_EXISTS, definition.getId())))
                .orElse(StoreResult.success());
    }

    @Override
    public StoreResult<Void> update(ContractDefinition definition) {
        var prev = cache.replace(ParticipantResourceKey.of(definition.getParticipantContextId(), definition.getId()), definition);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>success())
                .orElse(StoreResult.notFound(format(CONTRACT_DEFINITION_NOT_FOUND, definition.getId())));
    }

    @Override
    public StoreResult<ContractDefinition> deleteById(String participantContextId, String id) {
        var prev = cache.remove(ParticipantResourceKey.of(participantContextId, id));
        return Optional.ofNullable(prev)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound(format(CONTRACT_DEFINITION_NOT_FOUND, id)));
    }

}
