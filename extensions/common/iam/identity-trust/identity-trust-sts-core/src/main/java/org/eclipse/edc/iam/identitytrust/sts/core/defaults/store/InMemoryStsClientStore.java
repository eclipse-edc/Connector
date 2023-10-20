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

package org.eclipse.edc.iam.identitytrust.sts.core.defaults.store;

import org.eclipse.edc.iam.identitytrust.sts.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.store.StsClientStore;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * In memory implementation of {@link StsClientStore}
 */
public class InMemoryStsClientStore implements StsClientStore {

    private final Map<String, StsClient> clients = new ConcurrentHashMap<>();

    @Override
    public StoreResult<StsClient> create(StsClient client) {
        return Optional.ofNullable(clients.putIfAbsent(client.getId(), client))
                .map(old -> StoreResult.<StsClient>alreadyExists(format("Client with id %s already exists", client.getId())))
                .orElseGet(() -> StoreResult.success(client));
    }

    @Override
    public StoreResult<StsClient> findById(String id) {
        return Optional.ofNullable(clients.get(id))
                .map(StoreResult::success)
                .orElseGet(() -> StoreResult.notFound(format("Client with id %s not found.", id)));
    }
}
