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

package org.eclipse.edc.connector.dataplane.selector.service;

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.AVAILABLE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.REGISTERED;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.UNAVAILABLE;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.UNREGISTERED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataPlaneSelectorServiceImplTest {

    private final DataPlaneInstanceStore store = mock();
    private final SelectionStrategyRegistry selectionStrategyRegistry = mock();
    private final DataPlaneSelectorService service = new DataPlaneSelectorServiceImpl(store, selectionStrategyRegistry, new NoopTransactionContext());

    @Nested
    class Select {

        @Test
        void select_shouldUseChosenSelector() {
            var instances = range(0, 10)
                    .mapToObj(i -> createInstanceBuilder("instance" + i)
                            .allowedSourceType("srcTestType")
                            .allowedTransferType("transferType")
                            .state(AVAILABLE.code())
                            .build())
                    .toList();
            when(store.getAll()).thenAnswer(i -> instances.stream());
            SelectionStrategy selectionStrategy = mock();
            when(selectionStrategy.apply(any())).thenAnswer(it -> instances.get(0));
            when(selectionStrategyRegistry.find(any())).thenReturn(selectionStrategy);

            var result = service.select("strategy", dataPlane -> dataPlane.canHandle(createAddress("srcTestType"), "transferType"));

            assertThat(result).isSucceeded().extracting(DataPlaneInstance::getId).isEqualTo("instance0");
            verify(selectionStrategyRegistry).find("strategy");
        }

        @Test
        void select_shouldExcludeInstancesNotAvailable() {
            var availableInstance = createInstanceBuilder("available").state(AVAILABLE.code())
                    .allowedSourceType("srcTestType").allowedTransferType("transferType").build();
            var unavailableInstance = createInstanceBuilder("unavailable").state(UNAVAILABLE.code())
                    .allowedSourceType("srcTestType").allowedTransferType("transferType").build();
            when(store.getAll()).thenReturn(Stream.of(availableInstance, unavailableInstance));
            SelectionStrategy selectionStrategy = mock();
            when(selectionStrategy.apply(any())).thenAnswer(it -> availableInstance);
            when(selectionStrategyRegistry.find(any())).thenReturn(selectionStrategy);

            service.select("strategy", dataPlane -> dataPlane.canHandle(createAddress("srcTestType"), "transferType"));

            verify(selectionStrategy).apply(List.of(availableInstance));
        }

        @Test
        void select_shouldReturnBadRequest_whenStrategyNotFound() {
            var instances = range(0, 10)
                    .mapToObj(i -> createInstanceBuilder("instance" + i).build())
                    .toList();
            when(store.getAll()).thenReturn(instances.stream());
            when(selectionStrategyRegistry.find(any())).thenReturn(null);

            var result = service.select("strategy", dataPlane -> dataPlane.canHandle(createAddress("srcTestType"), "transferType"));

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        }

        @Test
        void select_shouldReturnNotFound_whenInstanceNotFound() {
            when(store.getAll()).thenReturn(Stream.empty());
            when(selectionStrategyRegistry.find(any())).thenReturn(mock());

            var result = service.select("strategy", dataPlane -> dataPlane.canHandle(createAddress("srcTestType"), "transferType"));

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDelete() {
            var instanceId = UUID.randomUUID().toString();
            var instance = createInstanceBuilder(instanceId).build();
            when(store.deleteById(any())).thenReturn(StoreResult.success(instance));

            var result = service.delete(instanceId);

            assertThat(result).isSucceeded().isNull();
        }

        @Test
        void shouldReturnNotFound_whenInstanceIsNotFound() {
            var instanceId = UUID.randomUUID().toString();
            when(store.deleteById(any())).thenReturn(StoreResult.notFound("not found"));

            var result = service.delete(instanceId);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }

    }

    @Nested
    class FindById {

        @Test
        void shouldReturnInstance() {
            var instance = createInstanceBuilder("instanceId").build();
            when(store.findById(any())).thenReturn(instance);

            var result = service.findById("instanceId");

            assertThat(result).isSucceeded().isSameAs(instance);
        }

        @Test
        void shouldFail_whenInstanceDoesNotExist() {
            when(store.findById(any())).thenReturn(null);

            var result = service.findById("any");

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    class AddInstance {
        @Test
        void shouldSaveRegisteredInstance() {
            var instance = DataPlaneInstance.Builder.newInstance().url("http://any").build();

            var result = service.addInstance(instance);

            assertThat(result).isSucceeded();
            verify(store).save(argThat(it -> it.getState() == REGISTERED.code()));
        }
    }

    @Nested
    class Unregister {
        @Test
        void shouldUnregisterInstance() {
            var instance = DataPlaneInstance.Builder.newInstance().url("http://any").build();
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.success(instance));

            var result = service.unregister(UUID.randomUUID().toString());

            assertThat(result).isSucceeded();
            verify(store).save(argThat(it -> it.getState() == UNREGISTERED.code()));
        }

        @Test
        void shouldFail_whenLeaseFails() {
            when(store.findByIdAndLease(any())).thenReturn(StoreResult.alreadyLeased("already leased"));

            var result = service.unregister(UUID.randomUUID().toString());

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
            verify(store, never()).save(any());
        }
    }

    private DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://any");
    }

    private DataAddress createAddress(String type) {
        return DataAddress.Builder.newInstance()
                .type(type)
                .keyName("key-name")
                .property("someprop", "someval")
                .build();
    }
}
