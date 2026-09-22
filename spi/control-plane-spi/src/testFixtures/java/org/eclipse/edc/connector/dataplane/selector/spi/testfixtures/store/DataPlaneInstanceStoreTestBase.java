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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.UNREGISTERED;
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
                .participantContextId("participantContextId")
                .allowedSourceType("source-type")
                .allowedTransferType("transfer-type")
                .destinationProvisionTypes(Set.of("provision-type"))
                .label("label")
                .property("name", "name")
                .authorizationProfile(new AuthorizationProfile("test-type", Map.of("key", "value")));
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

        @Test
        void query_byState() {
            var registered = createInstanceBuilder("test-id").build();
            registered.transitionToRegistered();
            var unregistered = createInstanceBuilder("test-id-2").build();
            unregistered.transitionToUnregistered();

            var store = getStore();

            store.save(registered);
            store.save(unregistered);

            var query = QuerySpec.Builder.newInstance().filter(new Criterion("state", "=", UNREGISTERED.code())).build();

            assertThat(store.query(query)).hasSize(1).first().extracting(DataPlaneInstance::getId).isEqualTo("test-id-2");
        }

        @Test
        void query_byAllowedTransferTypeContains() {
            var doc1 = createInstanceBuilder("test-id").allowedTransferType("HttpData-PULL").build();
            var doc2 = createInstanceBuilder("test-id-2").allowedTransferType("HttpData-PUSH").build();

            var store = getStore();

            store.save(doc1);
            store.save(doc2);

            var query = QuerySpec.Builder.newInstance().filter(new Criterion("allowedTransferTypes", "contains", "HttpData-PUSH")).build();

            assertThat(store.query(query)).hasSize(1).first().extracting(DataPlaneInstance::getId).isEqualTo("test-id-2");
        }

        @Test
        void query_byLabelContains() {
            var doc1 = createInstanceBuilder("test-id").label("eu").build();
            var doc2 = createInstanceBuilder("test-id-2").label("us").build();

            var store = getStore();

            store.save(doc1);
            store.save(doc2);

            var query = QuerySpec.Builder.newInstance().filter(new Criterion("labels", "contains", "us")).build();

            assertThat(store.query(query)).hasSize(1).first().extracting(DataPlaneInstance::getId).isEqualTo("test-id-2");
        }

        @Test
        void query_byProperty() {
            var doc1 = createInstanceWithProperty("test-id", "first");
            var doc2 = createInstanceWithProperty("test-id-2", "second");

            var store = getStore();

            store.save(doc1);
            store.save(doc2);

            var query = QuerySpec.Builder.newInstance().filter(new Criterion("properties.name", "=", "second")).build();

            assertThat(store.query(query)).hasSize(1).first().extracting(DataPlaneInstance::getId).isEqualTo("test-id-2");
        }

        @Test
        void query_sortByCreatedAt() {
            var doc1 = createInstanceBuilder("test-id").createdAt(100).build();
            var doc2 = createInstanceBuilder("test-id-2").createdAt(200).build();
            var doc3 = createInstanceBuilder("test-id-3").createdAt(300).build();

            var store = getStore();

            store.save(doc1);
            store.save(doc2);
            store.save(doc3);

            var query = QuerySpec.Builder.newInstance().sortField("createdAt").sortOrder(SortOrder.DESC).build();

            assertThat(store.query(query)).extracting(DataPlaneInstance::getId).containsExactly("test-id-3", "test-id-2", "test-id");
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

        @Test
        void shouldPersistStateAndTimestamps() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString()).build();
            entry.transitionToRegistered();
            getStore().save(entry);

            var result = getStore().findById(entry.getId());

            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(REGISTERED.code());
            assertThat(result.getStateTimestamp()).isEqualTo(entry.getStateTimestamp());
            assertThat(result.getUpdatedAt()).isEqualTo(entry.getUpdatedAt());
            assertThat(result.getCreatedAt()).isEqualTo(entry.getCreatedAt());
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
