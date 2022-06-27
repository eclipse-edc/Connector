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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okio.BufferedSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamingRequestBodyTest {
    private static final byte[] CONTENT = "123".getBytes();

    @Test
    void verifyStreamingTransfer() throws IOException {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(CONTENT));

        var sink = mock(BufferedSink.class);
        var outputStream = new ByteArrayOutputStream();

        when(sink.outputStream()).thenReturn(outputStream);

        var body = new StreamingRequestBody(part, HttpDataAddress.OCTET_STREAM);
        body.writeTo(sink);

        assertThat(outputStream.toByteArray()).isEqualTo(CONTENT);
    }
}
