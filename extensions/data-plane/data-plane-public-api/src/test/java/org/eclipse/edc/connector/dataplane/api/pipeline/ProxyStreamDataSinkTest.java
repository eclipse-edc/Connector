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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.stream.Stream;

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
        var dataSource = new TestDataSource("application/something", new ByteArrayInputStream(data));
        var dataSink = new ProxyStreamDataSink(randomUUID().toString(), monitor);

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(5, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded().isInstanceOf(ProxyStreamPayload.class)
                    .asInstanceOf(type(ProxyStreamPayload.class))
                    .satisfies(payload -> {
                        assertThat(payload.inputStream()).hasBinaryContent("bar".getBytes());
                        assertThat(payload.contentType()).isEqualTo("application/something");
                    });
        });
    }

    private record TestDataSource(String mediaType, InputStream inputStream) implements DataSource, DataSource.Part {

        @Override
        public StreamResult<Stream<Part>> openPartStream() {
            return StreamResult.success(Stream.of(this));
        }

        @Override
        public String name() {
            return UUID.randomUUID().toString();
        }

        @Override
        public InputStream openStream() {
            return inputStream;
        }
    }

}
