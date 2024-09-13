/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.defaults.store;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
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
 * In memory implementation of {@link StsClientStore}
 */
public class InMemoryStsClientStore implements StsClientStore {

    // we store it by clientId
    private final Map<String, StsClient> clients = new ConcurrentHashMap<>();
    private final QueryResolver<StsClient> queryResolver;


    public InMemoryStsClientStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(StsClient.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<StsClient> create(StsClient client) {
        return Optional.ofNullable(clients.putIfAbsent(client.getClientId(), client))
                .map(old -> StoreResult.<StsClient>alreadyExists(format(CLIENT_EXISTS_TEMPLATE, client.getClientId())))
                .orElseGet(() -> StoreResult.success(client));
    }

    @Override
    public StoreResult<Void> update(StsClient stsClient) {
        var prev = clients.replace(stsClient.getClientId(), stsClient);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>success())
                .orElse(StoreResult.notFound(format(CLIENT_NOT_FOUND_BY_ID_TEMPLATE, stsClient.getId())));
    }

    @Override
    public @NotNull Stream<StsClient> findAll(QuerySpec spec) {
        return queryResolver.query(clients.values().stream(), spec);
    }

    @Override
    public StoreResult<StsClient> findById(String id) {
        return clients.values().stream()
                .filter(client -> client.getId().equals(id))
                .findFirst()
                .map(StoreResult::success)
                .orElseGet(() -> StoreResult.notFound(format(CLIENT_NOT_FOUND_BY_ID_TEMPLATE, id)));
    }

    @Override
    public StoreResult<StsClient> findByClientId(String clientId) {
        return Optional.ofNullable(clients.get(clientId))
                .map(StoreResult::success)
                .orElseGet(() -> StoreResult.notFound(format(CLIENT_NOT_FOUND_BY_CLIENT_ID_TEMPLATE, clientId)));
    }

    @Override
    public StoreResult<StsClient> deleteById(String id) {
        return findById(id)
                .onSuccess(client -> clients.remove(client.getClientId()));
    }
}
