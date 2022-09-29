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

package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.boot.system.testextensions.BaseExtension;
import org.eclipse.dataspaceconnector.boot.system.testextensions.BaseRuntimeFixture;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BaseRuntimeTest {


    private BaseRuntimeFixture runtime;

    @AfterEach
    void teardown() {
        if (runtime != null) {
            runtime.stop();
        }
    }

    @Test
    void baseRuntime_shouldBoot() {

        var monitor = mock(Monitor.class);

        runtime = new BaseRuntimeFixture(monitor, List.of(new BaseExtension()));

        runtime.start();

        verify(monitor, never()).severe(anyString(), any());

    }

    @Test
    void baseRuntime_shouldNotBootWithoutExtensions() {

        var monitor = mock(Monitor.class);

        var runtime = new BaseRuntimeFixture(monitor);

        runtime.start();

        verify(monitor).severe(startsWith("Error booting runtime:"), any(EdcException.class));

    }

    @Test
    void baseRuntime_shouldNotBootWithException() {
        var monitor = mock(Monitor.class);
        var extension = spy(new BaseExtension());

        doThrow(new EdcException("Failed to start base extension")).when(extension).start();

        var runtime = new BaseRuntimeFixture(monitor, List.of(extension));

        runtime.start();

        verify(monitor).severe(startsWith("Error booting runtime: Failed to start base extension"), any(EdcException.class));

    }


}
