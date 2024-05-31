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

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.entity.MutableEntity;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.AVAILABLE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.hamcrest.Matchers.hasSize;


public abstract class DataPlaneInstanceStoreTestBase {

    protected static final String CONNECTOR_NAME = "test-connector";

    @Test
    void save() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);
        assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void save_withAllowedTransferTypes() {
        var inst = createInstanceBuilder("test-id").allowedTransferType("transfer-type").build();
        getStore().create(inst);
        assertThat(getStore().getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(inst);
    }

    @Test
    void update_whenExists_shouldUpdate() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);


        var inst2 = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url("http://somewhere.other:9876/api/v2") //different URL
                .build();

        var result = getStore().update(inst2);

        assertThat(result.succeeded()).isTrue();
        assertThat(getStore().getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(inst2);
    }

    @Test
    void save_shouldReturnCustomInstance() {
        var custom = createInstanceWithProperty("test-id", "name");

        getStore().create(custom);

        var customInstance = getStore().findById(custom.getId());

        assertThat(customInstance)
                .isInstanceOf(DataPlaneInstance.class)
                .usingRecursiveComparison()
                .isEqualTo(custom);
    }

    @Test
    void findById() {
        var inst = createInstanceBuilder("test-id").build();
        getStore().create(inst);

        assertThat(getStore().findById("test-id")).usingRecursiveComparison().isEqualTo(inst);
    }

    @Test
    void findById_notExists() {
        assertThat(getStore().findById("not-exist")).isNull();
    }

    @Test
    void getAll() {
        var doc1 = createInstanceWithProperty("test-id", "name");
        var doc2 = createInstanceWithProperty("test-id-2", "name");

        var store = getStore();

        store.create(doc1);
        store.create(doc2);

        var foundItems = store.getAll();

        assertThat(foundItems).isNotNull().hasSize(2);
    }

    @Nested
    class Create {

        @Test
        void shouldStoreEntity_whenItDoesNotAlreadyExist() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString()).build();
            getStore().save(entry);

            var result = getStore().findById(entry.getId());

            assertThat(result).isNotNull().usingRecursiveComparison().isEqualTo(entry);
            assertThat(result.getCreatedAt()).isGreaterThan(0);
        }

        @Test
        void shouldUpdate_whenEntityAlreadyExist() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString()).build();
            getStore().save(entry);

            entry.transitionToAvailable();
            getStore().save(entry);

            var result = getStore().findById(entry.getId());

            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(AVAILABLE.code());
        }
    }

    @Nested
    class NextNotLeased {
        @Test
        void shouldReturnNotLeasedItems() {
            var state = REGISTERED;
            var all = range(0, 5)
                    .mapToObj(i -> createInstanceBuilder("id-" + i).state(state.code()).build())
                    .peek(getStore()::save)
                    .peek(this::delayByTenMillis)
                    .toList();

            var leased = getStore().nextNotLeased(2, hasState(state.code()));

            assertThat(leased).hasSize(2).extracting(StatefulEntity::getId)
                    .isSubsetOf(all.stream().map(Entity::getId).toList())
                    .allMatch(id -> isLeasedBy(id, CONNECTOR_NAME));

            assertThat(leased).extracting(MutableEntity::getUpdatedAt).isSorted();
        }

        @Test
        void shouldReturnFreeEntities() {
            var state = REGISTERED;
            var all = range(0, 5)
                    .mapToObj(i -> createInstanceBuilder("id-" + i).state(state.code()).build())
                    .peek(getStore()::save)
                    .toList();

            var firstLeased = getStore().nextNotLeased(2, hasState(state.code()));
            var leased = getStore().nextNotLeased(2, hasState(state.code()));

            assertThat(leased.stream().map(Entity::getId)).hasSize(2)
                    .isSubsetOf(all.stream().map(Entity::getId).toList())
                    .doesNotContainAnyElementsOf(firstLeased.stream().map(Entity::getId).toList());
        }

        @Test
        void shouldReturnFreeItemInTheExpectedState() {
            range(0, 5)
                    .mapToObj(i -> createInstanceBuilder("id-" + i).state(REGISTERED.code()).build())
                    .forEach(getStore()::save);

            var leased = getStore().nextNotLeased(2, hasState(AVAILABLE.code()));

            assertThat(leased).isEmpty();
        }

        @Test
        void shouldLeaseAgainAfterTimePassed() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString()).state(REGISTERED.code()).build();
            getStore().save(entry);

            leaseEntity(entry.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

            await().atMost(Duration.ofMillis(500))
                    .until(() -> getStore().nextNotLeased(1, hasState(REGISTERED.code())), hasSize(1));
        }

        @Test
        void shouldReturnReleasedEntityByUpdate() {
            var entry = createInstanceBuilder(UUID.randomUUID().toString()).state(REGISTERED.code()).build();
            getStore().save(entry);

            var firstLeased = getStore().nextNotLeased(1, hasState(REGISTERED.code()));
            assertThat(firstLeased).hasSize(1);

            var secondLeased = getStore().nextNotLeased(1, hasState(REGISTERED.code()));
            assertThat(secondLeased).isEmpty();

            getStore().save(firstLeased.get(0));

            var thirdLeased = getStore().nextNotLeased(1, hasState(REGISTERED.code()));
            assertThat(thirdLeased).hasSize(1);
        }

        private void delayByTenMillis(StatefulEntity<?> t) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                // noop
            }
            t.updateStateTimestamp();
        }
    }

    @Nested
    class FindByIdAndLease {
        @Test
        void shouldReturnTheEntityAndLeaseIt() {
            var id = UUID.randomUUID().toString();
            getStore().save(createInstanceBuilder(id).state(REGISTERED.code()).build());

            var result = getStore().findByIdAndLease(id);

            assertThat(result).isSucceeded();
            assertThat(isLeasedBy(id, CONNECTOR_NAME)).isTrue();
        }

        @Test
        void shouldReturnNotFound_whenEntityDoesNotExist() {
            var result = getStore().findByIdAndLease("unexistent");

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldReturnAlreadyLeased_whenEntityIsAlreadyLeased() {
            var id = UUID.randomUUID().toString();
            getStore().save(createInstanceBuilder(id).state(REGISTERED.code()).build());
            leaseEntity(id, "other owner");

            var result = getStore().findByIdAndLease(id);

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_LEASED);
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldDeleteDataPlaneInstanceById() {
            var id = UUID.randomUUID().toString();
            var instance = createInstanceBuilder(id).build();
            getStore().create(instance);

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

    protected abstract DataPlaneInstanceStore getStore();

    protected abstract void leaseEntity(String entityId, String owner, Duration duration);

    protected void leaseEntity(String entityId, String owner) {
        leaseEntity(entityId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLeasedBy(String entityId, String owner);

    private DataPlaneInstance createInstanceWithProperty(String id, String name) {
        return createInstanceBuilder(id)
                .property("name", name)
                .build();
    }

    private DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1");
    }

}
