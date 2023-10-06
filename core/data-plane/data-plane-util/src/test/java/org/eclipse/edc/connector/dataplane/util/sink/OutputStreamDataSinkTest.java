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

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class OutputStreamDataSinkTest {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Monitor monitor = mock();

    @Test
    void verifySend() {
        var data = "bar".getBytes();
        var dataSource = new InputStreamDataSource("foo", new ByteArrayInputStream(data));
        var dataSink = new OutputStreamDataSink(randomUUID().toString(), executor, monitor);

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(5, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded().matches("bar"::equals);
        });
    }

}
