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

package org.eclipse.edc.connector.controlplane.defaults.storage.dataspaceprofile;

import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * An in-memory, threadsafe dataspace profile store. This implementation is intended for testing purposes only.
 */
public class InMemoryDataspaceProfileStore implements DataspaceProfileStore {

    private final Map<String, DataspaceProfile> profilesByName = new ConcurrentHashMap<>();
    private final QueryResolver<DataspaceProfile> queryResolver;

    public InMemoryDataspaceProfileStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(DataspaceProfile.class, criterionOperatorRegistry);
    }

    @Override
    public DataspaceProfile findById(String name) {
        try {
            return profilesByName.get(name);
        } catch (Exception e) {
            throw new EdcPersistenceException(format("Finding dataspace profile by name %s failed.", name), e);
        }
    }

    @Override
    public Stream<DataspaceProfile> findAll(QuerySpec spec) {
        return queryResolver.query(profilesByName.values().stream(), spec);
    }

    @Override
    public StoreResult<DataspaceProfile> create(DataspaceProfile profile) {
        var prev = profilesByName.putIfAbsent(profile.getName(), profile);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<DataspaceProfile>alreadyExists(format(PROFILE_ALREADY_EXISTS, profile.getName())))
                .orElse(StoreResult.success(profile));
    }

    @Override
    public StoreResult<DataspaceProfile> update(DataspaceProfile profile) {
        var prev = profilesByName.replace(profile.getName(), profile);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.success(profile))
                .orElse(StoreResult.notFound(format(PROFILE_NOT_FOUND, profile.getName())));
    }

    @Override
    public StoreResult<DataspaceProfile> delete(String name) {
        var prev = profilesByName.remove(name);
        return Optional.ofNullable(prev)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound(format(PROFILE_NOT_FOUND, name)));
    }

}
