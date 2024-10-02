/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.dispatcher.delegate;

import okhttp3.ResponseBody;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ByteArrayBodyExtractorTest {

    private final ByteArrayBodyExtractor extractor = new ByteArrayBodyExtractor();

    @Test
    void shouldReturnBodyAsBytes() throws IOException {
        var responseBody = mock(ResponseBody.class);
        var bytes = "test".getBytes();
        when(responseBody.bytes()).thenReturn(bytes);

        var result = extractor.extractBody(responseBody, "protocol");

        assertThat(result).isEqualTo(bytes);
    }

    @Test
    void shouldReturnNull_whenBodyIsNull() {
        var result = extractor.extractBody(null, "protocol");

        assertThat(result).isNull();
    }

    @Test
    void shouldThrowException_whenCannotExtractBytes() throws IOException {
        var responseBody = mock(ResponseBody.class);
        when(responseBody.bytes()).thenThrow(new IOException());

        assertThatThrownBy(() -> extractor.extractBody(responseBody, "protocol")).isInstanceOf(EdcException.class);
    }

}
