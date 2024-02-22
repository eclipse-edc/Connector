/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.store;

import org.eclipse.edc.connector.core.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-mem implementation of the {@link AccessTokenDataStore} based on a {@link ConcurrentHashMap}.
 */
public class InMemoryAccessTokenDataStore implements AccessTokenDataStore {
    private final Map<String, AccessTokenData> store = new ConcurrentHashMap<>();
    private final QueryResolver<AccessTokenData> queryResolver;

    public InMemoryAccessTokenDataStore(CriterionOperatorRegistry operatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(AccessTokenData.class, operatorRegistry);
    }

    @Override
    public AccessTokenData getById(String id) {
        return store.get(id);
    }

    @Override
    public StoreResult<Void> store(AccessTokenData accessTokenData) {

        var prev = store.putIfAbsent(accessTokenData.id(), accessTokenData);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>alreadyExists(OBJECT_EXISTS.formatted(accessTokenData.id())))
                .orElse(StoreResult.success());
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        var prev = store.remove(id);
        return Optional.ofNullable(prev)
                .map(p -> StoreResult.<Void>success())
                .orElse(StoreResult.notFound(OBJECT_NOT_FOUND.formatted(id)));
    }

    @Override
    public Collection<AccessTokenData> query(QuerySpec querySpec) {
        return queryResolver.query(store.values().stream(), querySpec).toList();
    }

}
