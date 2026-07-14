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

package org.eclipse.edc.validator.registration.spi.store.testfixtures;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

public abstract class SchemaValidatorRegistrationStoreTestBase {

    protected abstract SchemaValidatorRegistrationStore getStore();

    protected SchemaValidatorRegistration registration(String id, String version, String validatedType, String schema) {
        return SchemaValidatorRegistration.Builder.newInstance()
                .id(id)
                .version(version)
                .validatedType(validatedType)
                .schema(schema)
                .profiles(List.of("custom"))
                .build();
    }

    private String randomId() {
        return UUID.randomUUID().toString();
    }

    @Test
    void create_findById() {
        var registration = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");

        var result = getStore().create(registration);

        assertThat(result).extracting(r -> r.getContent()).usingRecursiveComparison().isEqualTo(registration);
        assertThat(getStore().findById(registration.getId())).usingRecursiveComparison().isEqualTo(registration);
    }

    @Test
    void create_whenExists_shouldReturnAlreadyExists() {
        var registration = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");
        getStore().create(registration);

        var result = getStore().create(registration);

        assertThat(result).extracting(r -> r.reason()).isEqualTo(ALREADY_EXISTS);
    }

    @Test
    void findByVersionAndValidatedType() {
        var target = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");
        getStore().create(target);
        getStore().create(registration(randomId(), "v5", "PolicyDefinition", "https://example.com/schema/policy.json"));
        getStore().create(registration(randomId(), "v4", "Asset", "https://example.com/schema/asset-v4.json"));

        var result = getStore().findByVersionAndValidatedType("v5", "Asset");

        assertThat(result).hasSize(1).allSatisfy(r -> assertThat(r.getId()).isEqualTo(target.getId()));
        assertThat(getStore().findByVersionAndValidatedType("v5", "Missing")).isEmpty();
    }

    @Test
    void findById_whenMissing_shouldReturnNull() {
        assertThat(getStore().findById("missing")).isNull();
    }

    @Test
    void update() {
        var registration = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");
        getStore().create(registration);

        var updated = registration.toBuilder().schema("https://example.com/schema/asset-v2.json").build();
        var result = getStore().update(updated);

        assertThat(result.succeeded()).isTrue();
        assertThat(getStore().findById(registration.getId()).getSchema()).isEqualTo("https://example.com/schema/asset-v2.json");
    }

    @Test
    void update_whenMissing_shouldReturnNotFound() {
        var registration = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");

        var result = getStore().update(registration);

        assertThat(result).extracting(r -> r.reason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete() {
        var registration = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");
        getStore().create(registration);

        var result = getStore().delete(registration.getId());

        assertThat(result.succeeded()).isTrue();
        assertThat(getStore().findById(registration.getId())).isNull();
    }

    @Test
    void delete_whenMissing_shouldReturnNotFound() {
        var result = getStore().delete("missing");

        assertThat(result).extracting(r -> r.reason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void findAll() {
        IntStream.range(0, 5).forEach(i -> getStore().create(registration(randomId(), "v5", "Type" + i, "https://example.com/schema/" + i + ".json")));

        var result = getStore().findAll(QuerySpec.max()).toList();

        assertThat(result).hasSize(5);
    }

    @Test
    void findAll_withFilter() {
        var target = registration(randomId(), "v5", "Asset", "https://example.com/schema/asset.json");
        getStore().create(target);
        getStore().create(registration(randomId(), "v5", "PolicyDefinition", "https://example.com/schema/policy.json"));

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("validatedType", "=", "Asset"))
                .build();

        assertThat(getStore().findAll(query).toList()).hasSize(1)
                .allSatisfy(r -> assertThat(r.getId()).isEqualTo(target.getId()));
    }
}
