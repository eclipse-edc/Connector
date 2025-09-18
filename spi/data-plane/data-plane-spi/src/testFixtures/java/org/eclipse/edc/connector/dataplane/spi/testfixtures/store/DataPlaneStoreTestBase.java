/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.spi.testfixtures.store;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;
import static org.hamcrest.Matchers.hasSize;

public abstract class DataPlaneStoreTestBase {

    protected static final String CONNECTOR_NAME = "test-connector";

    protected abstract DataPlaneStore getStore();

    protected abstract void leaseEntity(String entityId, String owner, Duration duration);

    protected void leaseEntity(String entityId, String owner) {
        leaseEntity(entityId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLeasedBy(String entityId, String owner);

    /**
     * determines the amount of time (default = 500ms) before an async test using Awaitility fails. This may be useful if using remote
     * or non-self-contained databases.
     */
    protected Duration getTestTimeout() {
        return Duration.ofMillis(500);
    }

    @Nested
    class Create {

        @Test
        void shouldStoreEntity_whenItDoesNotAlreadyExist() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            var result = getStore().findById(dataFlow.getId());

            assertThat(result).isNotNull().usingRecursiveComparison().ignoringFields("resourceDefinitions").isEqualTo(dataFlow);
            assertThat(result.getCreatedAt()).isGreaterThan(0);
            assertThat(result.getResourceDefinitions()).hasSize(1).first().satisfies(actualProvisionResource -> {
                assertThat(actualProvisionResource).usingRecursiveComparison().ignoringFields("clock")
                        .isEqualTo(dataFlow.getResourceDefinitions().get(0));
            });
        }

        @Test
        void shouldUpdate_whenEntityAlreadyExist() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            dataFlow.transitToCompleted();
            getStore().save(dataFlow);

            var result = getStore().findById(dataFlow.getId());

            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(COMPLETED.code());
        }
    }

    @Nested
    class NextNotLeased {
        @Test
        void shouldReturnNotLeasedItems() {
            var state = RECEIVED;
            var all = range(0, 5)
                    .mapToObj(i -> createDataFlow("id-" + i, state))
                    .peek(getStore()::save)
                    .peek(this::delayByTenMillis)
                    .toList();

            var leased = getStore().nextNotLeased(2, hasState(state.code()));

            assertThat(leased).hasSize(2).extracting(DataFlow::getId)
                    .isSubsetOf(all.stream().map(Entity::getId).toList())
                    .allMatch(id -> isLeasedBy(id, CONNECTOR_NAME));

            assertThat(leased).extracting(StatefulEntity::getUpdatedAt).isSorted();
        }

        @Test
        void shouldReturnFreeEntities() {
            var state = RECEIVED;
            var all = range(0, 5)
                    .mapToObj(i -> createDataFlow("id-" + i, state))
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
                    .mapToObj(i -> createDataFlow("id-" + i, RECEIVED))
                    .forEach(getStore()::save);

            var leased = getStore().nextNotLeased(2, hasState(COMPLETED.code()));

            assertThat(leased).isEmpty();
        }

        @Test
        void shouldLeaseAgainAfterTimePassed() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            leaseEntity(dataFlow.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

            await().atMost(getTestTimeout())
                    .until(() -> getStore().nextNotLeased(1, hasState(RECEIVED.code())), hasSize(1));
        }

        @Test
        void shouldReturnReleasedEntityByUpdate() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            var firstLeased = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(firstLeased).hasSize(1);

            var secondLeased = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(secondLeased).isEmpty();

            getStore().save(firstLeased.get(0));

            var thirdLeased = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(thirdLeased).hasSize(1);
        }

        @Test
        void shouldLeaseOrderByStateTimestamp() {

            var all = range(0, 10)
                    .mapToObj(i -> createDataFlow("id-" + i, RECEIVED))
                    .peek(getStore()::save)
                    .toList();

            all.stream().limit(5)
                    .peek(this::delayByTenMillis)
                    .sorted(Comparator.comparing(DataFlow::getStateTimestamp).reversed())
                    .forEach(f -> getStore().save(f));

            var elements = getStore().nextNotLeased(10, hasState(RECEIVED.code()));
            assertThat(elements).hasSize(10).extracting(DataFlow::getStateTimestamp).isSorted();
        }

        @Test
        void shouldFilterByFlowType() {
            var pull = createDataFlowBuilder().transferType(new TransferType("Any", PULL)).build();
            var push = createDataFlowBuilder().transferType(new TransferType("Any", PUSH)).build();
            getStore().save(pull);
            getStore().save(push);

            var leased = getStore().nextNotLeased(2, new Criterion("transferType.flowType", "=", "PUSH"));

            assertThat(leased).hasSize(1).first().extracting(DataFlow::getId)
                    .isEqualTo(push.getId());
        }

        @Test
        void shouldFilterByRuntimeId() {
            var runtime1 = createDataFlowBuilder().runtimeId("runtime1").build();
            var runtime2 = createDataFlowBuilder().runtimeId("runtime2").build();
            getStore().save(runtime1);
            getStore().save(runtime2);

            var leased = getStore().nextNotLeased(2, new Criterion("runtimeId", "=", "runtime2"));

            assertThat(leased).hasSize(1).first().extracting(DataFlow::getId)
                    .isEqualTo(runtime2.getId());
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
            getStore().save(createDataFlow(id, RECEIVED));

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
            getStore().save(createDataFlow(id, RECEIVED));
            leaseEntity(id, "other owner");

            var result = getStore().findByIdAndLease(id);

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_LEASED);
        }
    }

    private DataFlow createDataFlow(String id, DataFlowStates state) {
        return createDataFlowBuilder()
                .id(id).state(state.code())
                .build();
    }

    private DataFlow.Builder createDataFlowBuilder() {
        var provisionResource = ProvisionResource.Builder.newInstance().flowId("flowId").build();
        provisionResource.transitionProvisioned(ProvisionedResource.Builder.from(provisionResource).dataAddress(DataAddress.Builder.newInstance().type("any").build()).build());
        provisionResource.transitionDeprovisioned(DeprovisionedResource.Builder.from(provisionResource).build());
        return DataFlow.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .callbackAddress(URI.create("http://any"))
                .source(DataAddress.Builder.newInstance().type("src-type").build())
                .destination(DataAddress.Builder.newInstance().type("dest-type").build())
                .state(STARTED.code())
                .transferType(new TransferType("transferType", PUSH))
                .runtimeId("runtimeId")
                .resourceDefinitions(List.of(provisionResource));
    }

}
