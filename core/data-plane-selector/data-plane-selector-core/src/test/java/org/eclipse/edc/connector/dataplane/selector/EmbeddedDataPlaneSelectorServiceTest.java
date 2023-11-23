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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.TestFunctions.createAddress;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmbeddedDataPlaneSelectorServiceTest {

    private final DataPlaneInstanceStore store = mock();
    private final SelectionStrategyRegistry selectionStrategyRegistry = mock();
    private final DataPlaneSelectorService selector = new EmbeddedDataPlaneSelectorService(store, selectionStrategyRegistry, mock());

    @Test
    void select_shouldUseChosenSelector() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, "srcTestType", "destTestType")).toList();
        when(store.getAll()).thenReturn(instances.stream());
        SelectionStrategy selectionStrategy = mock();
        when(selectionStrategy.apply(any())).thenAnswer(it -> instances.get(0));
        when(selectionStrategyRegistry.find(any())).thenReturn(selectionStrategy);

        var result = selector.select(createAddress("srcTestType"), createAddress("destTestType"), "strategy");

        assertThat(result).isNotNull().extracting(DataPlaneInstance::getId).isEqualTo("instance0");
        verify(selectionStrategyRegistry).find("strategy");
    }

    @Test
    void select_shouldThrowException_whenStrategyNotFound() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, "srcTestType", "destTestType")).toList();
        when(store.getAll()).thenReturn(instances.stream());
        when(selectionStrategyRegistry.find(any())).thenReturn(null);

        assertThatThrownBy(() -> selector.select(createAddress("srcTestType"), createAddress("destTestType"), "strategy"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DataPlaneInstance createInstanceMock(String id, String srcType, String destType) {
        return DataPlaneInstance.Builder.newInstance()
                .url("http://any")
                .id(id)
                .allowedSourceType(srcType)
                .allowedDestType(destType)
                .build();
    }
}
