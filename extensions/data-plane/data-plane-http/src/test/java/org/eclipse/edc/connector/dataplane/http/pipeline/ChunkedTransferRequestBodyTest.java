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

class ChunkedTransferRequestBodyTest {

    @Test
    void verifyStreamingTransfer() throws IOException {
        var content = "Test content";
        var sink = mock(BufferedSink.class);
        var outputStream = new ByteArrayOutputStream();

        when(sink.outputStream()).thenReturn(outputStream);

        var body = new ChunkedTransferRequestBody(() -> new ByteArrayInputStream(content.getBytes()), HttpDataAddress.OCTET_STREAM);
        body.writeTo(sink);

        assertThat(outputStream).hasToString(content);
    }
}
