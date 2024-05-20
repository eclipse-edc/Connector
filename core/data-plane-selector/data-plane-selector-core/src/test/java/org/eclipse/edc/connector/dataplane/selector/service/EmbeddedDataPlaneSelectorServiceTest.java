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

import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmbeddedDataPlaneSelectorServiceTest {

    private final DataPlaneInstanceStore store = mock();
    private final SelectionStrategyRegistry selectionStrategyRegistry = mock();
    private final DataPlaneSelectorService service = new EmbeddedDataPlaneSelectorService(store, selectionStrategyRegistry, new NoopTransactionContext());

    @Test
    void select_shouldUseChosenSelector() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, "srcTestType", "destTestType")).toList();
        when(store.getAll()).thenReturn(instances.stream());
        SelectionStrategy selectionStrategy = mock();
        when(selectionStrategy.apply(any())).thenAnswer(it -> instances.get(0));
        when(selectionStrategyRegistry.find(any())).thenReturn(selectionStrategy);

        var result = service.select(createAddress("srcTestType"), "transferType", "strategy");

        assertThat(result).isSucceeded().extracting(DataPlaneInstance::getId).isEqualTo("instance0");
        verify(selectionStrategyRegistry).find("strategy");
    }

    @Test
    void select_shouldReturnBadRequest_whenStrategyNotFound() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, "srcTestType", "destTestType")).toList();
        when(store.getAll()).thenReturn(instances.stream());
        when(selectionStrategyRegistry.find(any())).thenReturn(null);

        var result = service.select(createAddress("srcTestType"), "transferType", "strategy");

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
    }

    @Test
    void select_shouldReturnNotFound_whenInstanceNotFound() {
        when(store.getAll()).thenReturn(Stream.empty());
        when(selectionStrategyRegistry.find(any())).thenReturn(mock());

        var result = service.select(createAddress("srcTestType"), "transferType", "strategy");

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
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

    private DataPlaneInstance createInstanceMock(String id, String srcType, String destType) {
        return createInstanceBuilder(id)
                .allowedSourceType(srcType)
                .allowedDestType(destType)
                .build();
    }

    private DataPlaneInstance.Builder createInstanceBuilder(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://any");
    }

    private DataAddress createAddress(String type) {
        return DataAddress.Builder.newInstance()
                .type("test-type")
                .keyName(type)
                .property("someprop", "someval")
                .build();
    }
}
