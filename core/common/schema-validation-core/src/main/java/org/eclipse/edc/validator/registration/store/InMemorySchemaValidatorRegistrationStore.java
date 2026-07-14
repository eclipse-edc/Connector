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

package org.eclipse.edc.validator.registration.store;

import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * In-memory, threadsafe {@link SchemaValidatorRegistrationStore}. Intended as the default implementation.
 */
public class InMemorySchemaValidatorRegistrationStore implements SchemaValidatorRegistrationStore {

    private final Map<String, SchemaValidatorRegistration> byId = new ConcurrentHashMap<>();
    private final QueryResolver<SchemaValidatorRegistration> queryResolver;

    public InMemorySchemaValidatorRegistrationStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.queryResolver = new ReflectionBasedQueryResolver<>(SchemaValidatorRegistration.class, criterionOperatorRegistry);
    }

    @Override
    public SchemaValidatorRegistration findById(String id) {
        return byId.get(id);
    }

    @Override
    public List<SchemaValidatorRegistration> findByVersionAndValidatedType(String version, String validatedType) {
        return byId.values().stream()
                .filter(r -> r.getVersion().equals(version) && r.getValidatedType().equals(validatedType))
                .toList();
    }

    @Override
    public Stream<SchemaValidatorRegistration> findAll(QuerySpec spec) {
        return queryResolver.query(byId.values().stream(), spec);
    }

    @Override
    public StoreResult<SchemaValidatorRegistration> create(SchemaValidatorRegistration registration) {
        var prev = byId.putIfAbsent(registration.getId(), registration);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<SchemaValidatorRegistration>alreadyExists(format(ALREADY_EXISTS, registration.getId())))
                .orElse(StoreResult.success(registration));
    }

    @Override
    public StoreResult<SchemaValidatorRegistration> update(SchemaValidatorRegistration registration) {
        var prev = byId.replace(registration.getId(), registration);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.success(registration))
                .orElse(StoreResult.notFound(format(NOT_FOUND, registration.getId())));
    }

    @Override
    public StoreResult<SchemaValidatorRegistration> delete(String id) {
        var prev = byId.remove(id);
        return Optional.ofNullable(prev)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound(format(NOT_FOUND, id)));
    }
}
