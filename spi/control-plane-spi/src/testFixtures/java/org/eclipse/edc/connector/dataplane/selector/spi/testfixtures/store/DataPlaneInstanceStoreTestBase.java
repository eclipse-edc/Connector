/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.store;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;


public abstract class DataPlaneInstanceStoreTestBase {

    protected static final String CONNECTOR_NAME = "test-connector";

    protected abstract DataPlaneInstanceStore getStore();
    
    /**
     * determines the amount of time (default = 500ms) before an async test using Awaitility fails. This may be useful if using remote
     * or non-self-contained databases.
     */
    protected Duration getTestTimeout() {
        return Duration.ofMillis(500);
    }

    private DataPlaneInstance createInstanceWithProperty(String id, String name) {
        return createInstanceBuilder(id)
                .property("name", name)
                .build();
    }

    private DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1")
                .participantContextId("participantContextId");
    }

    @Nested
    class FindById {
        @Test
        void findById() {
            var inst = createInstanceBuilder("test-id").build();
            getStore().save(inst);

            assertThat(getStore().findById("test-id")).usingRecursiveComparison().isEqualTo(inst);
        }

        @Test
        void findById_notExists() {
            assertThat(getStore().findById("not-exist")).isNull();
        }

    }

    @Nested
    class GetAll {
        @Test
        void getAll() {
            var doc1 = createInstanceWithProperty("test-id", "name");
            var doc2 = createInstanceWithProperty("test-id-2", "name");

            var store = getStore();

            store.save(doc1);
            store.save(doc2);

            var foundItems = store.getAll();

            assertThat(foundItems).isNotNull().hasSize(2);
        }
    }

    @Nested
    class Query {
        @Test
        void query_participantContext() {
            var doc1 = createInstanceBuilder("test-id").participantContextId("participant1").build();
            var doc2 = createInstanceBuilder("test-id-2").participantContextId("participant2").build();

            var store = getStore();

            store.save(doc1);
            store.save(doc2);

            var foundItems = store.query(queryByParticipantContextId("participant1").build());

            assertThat(foundItems).isNotNull().hasSize(1);
        }
    }

    @Nested
    class Create {

        @Test
        void shouldStoreEntity_whenItDoesNotAlreadyExist() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString())
                    .allowedTransferType("transfer-type")
                    .allowedSourceType("source-type")
                    .label("label")
                    .authorizationProfile(new AuthorizationProfile("test-type", Map.of("key", "value")))
                    .build();
            getStore().save(entry);

            var result = getStore().findById(entry.getId());

            assertThat(result).isNotNull().usingRecursiveComparison().isEqualTo(entry);
            assertThat(result.getCreatedAt()).isGreaterThan(0);
        }

        @Test
        void shouldUpdate_whenEntityAlreadyExist() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString()).build();
            getStore().save(entry);

            entry.transitionToRegistered();
            getStore().save(entry);

            var result = getStore().findById(entry.getId());

            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(REGISTERED.code());
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldDeleteDataPlaneInstanceById() {
            var id = UUID.randomUUID().toString();
            var instance = createInstanceBuilder(id).build();
            getStore().save(instance);

            var result = getStore().deleteById(id);

            assertThat(result).isSucceeded().usingRecursiveComparison().isEqualTo(instance);
        }

        @Test
        void shouldFail_whenInstanceDoesNotExist() {
            var randomId = UUID.randomUUID().toString();

            var result = getStore().deleteById(randomId);

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

    }

}
