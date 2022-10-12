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

package org.eclipse.dataspaceconnector.dataplane.selector.client;

import org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelectorService;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class EmbeddedDataPlaneSelectorClientTest {

    private EmbeddedDataPlaneSelectorClient client;
    private DataPlaneSelectorService serviceMock;

    @BeforeEach
    void setUp() {
        serviceMock = mock(DataPlaneSelectorService.class);
        client = new EmbeddedDataPlaneSelectorClient(serviceMock);
    }

    @Test
    void getAll() {
        client.getAll();
        verify(serviceMock).getAll();
        verifyNoMoreInteractions(serviceMock);
    }

    @Test
    void find() {
        var src = DataAddress.Builder.newInstance().type("test-type").build();
        var dest = DataAddress.Builder.newInstance().type("test-type").build();
        client.find(src, dest);
        verify(serviceMock).select(eq(src), eq(dest));
        verifyNoMoreInteractions(serviceMock);
    }

    @Test
    void find_withStrategy() {
        var src = DataAddress.Builder.newInstance().type("test-type").build();
        var dest = DataAddress.Builder.newInstance().type("test-type").build();
        client.find(src, dest, "test-strategy");
        verify(serviceMock).select(eq(src), eq(dest), eq("test-strategy"));
        verifyNoMoreInteractions(serviceMock);
    }

}