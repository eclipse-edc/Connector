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

package org.eclipse.dataspaceconnector.dataplane.selector.core;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.selector.TestFunctions.createAddress;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataPlaneSelectorImplTest {

    private DataPlaneSelectorImpl selector;
    private DataPlaneInstanceStore storeMock;

    @BeforeEach
    void setUp() {
        storeMock = mock(DataPlaneInstanceStore.class);
        selector = new DataPlaneSelectorImpl(storeMock);
    }

    @Test
    void select() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, true));
        when(storeMock.getAll()).thenReturn(instances);

        var result = selector.select(createAddress("AzureStorzge"), createAddress("AzureStorzge"));

        assertThat(result).isNotNull();
    }

    @RepeatedTest(100)
    void select_someCanHandle() {
        var instances = Stream.of(
                createInstanceMock("instance0", false),
                createInstanceMock("instance1", true),
                createInstanceMock("instance2", true),
                createInstanceMock("instance3", false),
                createInstanceMock("instance4", false)
        );
        when(storeMock.getAll()).thenReturn(instances);

        var result = selector.select(createAddress("AzureStorage"), createAddress("FTP"));

        assertThat(result).isNotNull().extracting(DataPlaneInstance::getId).isIn("instance1", "instance2");
    }

    @Test
    void select_noneCanHandle() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, false));
        when(storeMock.getAll()).thenReturn(instances);

        var result = selector.select(createAddress("AzureStorzge"), createAddress("AmazonS3"));

        assertThat(result).isNull();
    }

    @Test
    void select_withSelectionStrategy() {
        var instances = IntStream.range(0, 10).mapToObj(i -> createInstanceMock("instance" + i, true));
        when(storeMock.getAll()).thenReturn(instances);

        var result = selector.select(createAddress("AzureStorage"), createAddress("http"), instances1 -> instances1.get(0));

        assertThat(result).isNotNull().extracting(DataPlaneInstance::getId).isEqualTo("instance0");
    }

    @Test
    void select_withSelectionStrategy_someCanHandle() {
        var instances = Stream.of(
                createInstanceMock("instance0", false),
                createInstanceMock("instance1", true),
                createInstanceMock("instance2", true),
                createInstanceMock("instance3", false),
                createInstanceMock("instance4", false)
        );
        when(storeMock.getAll()).thenReturn(instances);

        var result = selector.select(createAddress("AmazonS3"), createAddress("http"), instances1 -> instances1.get(0));

        assertThat(result).isNotNull().extracting(DataPlaneInstance::getId).isEqualTo("instance1");
    }

    @Test
    void select_verifyDefaultRandomSelection() {

    }

    private DataPlaneInstance createInstanceMock(String id, boolean canHandle) {
        var mock = mock(DataPlaneInstance.class);
        when(mock.getId()).thenReturn(id);
        when(mock.canHandle(any(), any())).thenReturn(canHandle);
        return mock;
    }
}