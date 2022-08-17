/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutputStreamDataSinkTest {
    private ExecutorService executor;
    private Monitor monitor;

    @Test
    void verifySend() throws Exception {
        var data = "bar".getBytes();
        var dataSource = new InputStreamDataSource("foo", new ByteArrayInputStream(data));

        var stream = new ByteArrayOutputStream();
        var dataSink = new OutputStreamDataSink(stream, executor, monitor);

        dataSink.transfer(dataSource).get(30, SECONDS);

        assertThat(stream.toByteArray()).isEqualTo(data);
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        monitor = mock(Monitor.class);
    }

}
