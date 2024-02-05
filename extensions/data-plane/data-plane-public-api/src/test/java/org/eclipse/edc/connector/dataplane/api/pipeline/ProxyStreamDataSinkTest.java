/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.api.pipeline;

import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class ProxyStreamDataSinkTest {

    private final Monitor monitor = mock();

    @Test
    void shouldReturnTheInputStream() {
        var data = "bar".getBytes();
        var dataSource = new InputStreamDataSource("foo", new ByteArrayInputStream(data));
        var dataSink = new ProxyStreamDataSink(randomUUID().toString(), monitor);

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(5, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded().isInstanceOf(ByteArrayInputStream.class).asInstanceOf(type(ByteArrayInputStream.class))
                    .matches(it -> it.available() > 0)
                    .extracting(ByteArrayInputStream::readAllBytes)
                    .satisfies(bytes -> assertThat(new String(bytes)).isEqualTo("bar"));
        });
    }

}
