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

package org.eclipse.edc.protocol.spi.store;

import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

public abstract class DataspaceProfileStoreTestBase {

    protected abstract DataspaceProfileStore getStore();

    private DataspaceProfile createProfile(String name) {
        return DataspaceProfile.Builder.newInstance()
                .name(name)
                .protocolVersion("2025-1")
                .path("/" + name)
                .binding("HTTPS")
                .namespace("https://w3id.org/dspace/2025/1/")
                .jsonLdContextsUrl(List.of("https://w3id.org/dspace/2025/1/context.jsonld"))
                .trustedIssuers(List.of(TrustedIssuer.Builder.newInstance()
                        .id("did:web:trusted.issuer")
                        .supportedTypes(List.of("MembershipCredential"))
                        .build()))
                .build();
    }

    private String getRandomName() {
        return UUID.randomUUID().toString();
    }

    @Nested
    class Create {

        @Test
        void notExisting() {
            var profile = createProfile(getRandomName());

            var result = getStore().create(profile);

            assertThat(result).extracting(StoreResult::succeeded).isEqualTo(true);
            var fromDb = getStore().findById(profile.getName());
            assertThat(fromDb).usingRecursiveComparison().isEqualTo(profile);
        }

        @Test
        void alreadyExists() {
            var name = getRandomName();
            var store = getStore();
            store.create(createProfile(name));

            var result = store.create(createProfile(name));

            assertThat(result.succeeded()).isFalse();
            assertThat(result.reason()).isEqualTo(ALREADY_EXISTS);
        }
    }

    @Nested
    class Update {

        @Test
        void notExisting() {
            var result = getStore().update(createProfile(getRandomName()));

            assertThat(result).extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
        }

        @Test
        void whenExists() {
            var name = getRandomName();
            var store = getStore();
            store.create(createProfile(name));

            var updated = store.findById(name).toBuilder().binding("HTTP").build();
            var result = store.update(updated);

            assertThat(result.succeeded()).isTrue();
            assertThat(store.findById(name).getBinding()).isEqualTo("HTTP");
        }
    }

    @Nested
    class FindById {

        @Test
        void whenPresent() {
            var profile = createProfile(getRandomName());
            getStore().create(profile);

            var fromDb = getStore().findById(profile.getName());

            assertThat(fromDb).usingRecursiveComparison().isEqualTo(profile);
        }

        @Test
        void whenNonexistent() {
            assertThat(getStore().findById("nonexistent")).isNull();
        }
    }

    @Nested
    class FindAll {

        @Test
        void withSpec() {
            IntStream.range(0, 10).forEach(i -> getStore().create(createProfile("profile-" + i)));

            var spec = QuerySpec.Builder.newInstance().limit(5).offset(2).build();

            assertThat(getStore().findAll(spec)).hasSize(5);
        }

        @Test
        void byName() {
            var profile = createProfile(getRandomName());
            getStore().create(profile);
            getStore().create(createProfile(getRandomName()));

            var spec = QuerySpec.Builder.newInstance().filter(new Criterion("name", "=", profile.getName())).build();

            assertThat(getStore().findAll(spec)).usingRecursiveFieldByFieldElementComparator().containsExactly(profile);
        }

        @Test
        void sorting_nonExistentProperty() {
            IntStream.range(0, 5).forEach(i -> getStore().create(createProfile("profile-" + i)));

            var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

            assertThatThrownBy(() -> getStore().findAll(query).toList()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class DeleteById {

        @Test
        void whenExists() {
            var profile = createProfile(getRandomName());
            var store = getStore();
            store.create(profile);

            var result = store.delete(profile.getName());

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(profile);
            assertThat(store.findById(profile.getName())).isNull();
        }

        @Test
        void whenNonexistent() {
            assertThat(getStore().delete("nonexistent")).extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
        }
    }
}
