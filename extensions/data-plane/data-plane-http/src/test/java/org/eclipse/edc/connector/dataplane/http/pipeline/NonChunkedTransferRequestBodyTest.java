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
 *       Siemens - add chunked parameter
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import okio.BufferedSink;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NonChunkedTransferRequestBodyTest {

    @Test
    void verifyTransferWhenDataAvailable() throws IOException {
        var content = "Test Content";
        var sink = mock(BufferedSink.class);
        var outputStream = new ByteArrayOutputStream();

        when(sink.outputStream()).thenReturn(outputStream);

        var body = new NonChunkedTransferRequestBody(() -> new ByteArrayInputStream(content.getBytes()), HttpDataAddress.OCTET_STREAM);

        assertThat(body.contentType()).hasToString(HttpDataAddress.OCTET_STREAM);
        assertThat(body.contentLength()).isEqualTo(content.getBytes().length);

        body.writeTo(sink);

        assertThat(outputStream).hasToString(content);
    }

    @Test
    void verifyTransferDataMissing() throws IOException {
        var sink = mock(BufferedSink.class);
        var outputStream = new ByteArrayOutputStream();

        when(sink.outputStream()).thenReturn(outputStream);

        var body = new NonChunkedTransferRequestBody(() -> new ByteArrayInputStream(new byte[0]), HttpDataAddress.OCTET_STREAM);

        assertThat(body.contentLength()).isZero();

        body.writeTo(sink);

        assertThat(outputStream).hasToString("");
    }
}
