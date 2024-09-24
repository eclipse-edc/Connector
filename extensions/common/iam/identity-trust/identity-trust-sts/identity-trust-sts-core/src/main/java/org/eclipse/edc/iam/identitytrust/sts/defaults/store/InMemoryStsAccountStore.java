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

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
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
 * In memory implementation of {@link StsAccountStore}
 */
public class InMemoryStsAccountStore implements StsAccountStore {

    // we store it by clientId
    private final Map<String, StsAccount> clients = new ConcurrentHashMap<>();
    private final QueryResolver<StsAccount> queryResolver;


    public InMemoryStsAccountStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(StsAccount.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<StsAccount> create(StsAccount client) {
        return Optional.ofNullable(clients.putIfAbsent(client.getClientId(), client))
                .map(old -> StoreResult.<StsAccount>alreadyExists(format(CLIENT_EXISTS_TEMPLATE, client.getClientId())))
                .orElseGet(() -> StoreResult.success(client));
    }

    @Override
    public StoreResult<Void> update(StsAccount stsAccount) {
        var prev = clients.replace(stsAccount.getClientId(), stsAccount);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>success())
                .orElse(StoreResult.notFound(format(CLIENT_NOT_FOUND_BY_ID_TEMPLATE, stsAccount.getId())));
    }

    @Override
    public @NotNull Stream<StsAccount> findAll(QuerySpec spec) {
        return queryResolver.query(clients.values().stream(), spec);
    }

    @Override
    public StoreResult<StsAccount> findById(String id) {
        return clients.values().stream()
                .filter(client -> client.getId().equals(id))
                .findFirst()
                .map(StoreResult::success)
                .orElseGet(() -> StoreResult.notFound(format(CLIENT_NOT_FOUND_BY_ID_TEMPLATE, id)));
    }

    @Override
    public StoreResult<StsAccount> findByClientId(String clientId) {
        return Optional.ofNullable(clients.get(clientId))
                .map(StoreResult::success)
                .orElseGet(() -> StoreResult.notFound(format(CLIENT_NOT_FOUND_BY_CLIENT_ID_TEMPLATE, clientId)));
    }

    @Override
    public StoreResult<StsAccount> deleteById(String id) {
        return findById(id)
                .onSuccess(client -> clients.remove(client.getClientId()));
    }
}
