/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.http.client;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseBodiesTest {

    private static final MediaType JSON = MediaType.get("application/json");

    @Test
    void readUtf8_shouldReturnContent_whenBelowMaxSize() throws IOException {
        var result = ResponseBodies.readUtf8(ResponseBody.create("{\"ä\":1}", JSON), 100);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("{\"ä\":1}");
    }

    @Test
    void readUtf8_shouldReturnContent_whenEqualToMaxSize() throws IOException {
        var result = ResponseBodies.readUtf8(ResponseBody.create("12345", JSON), 5);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo("12345");
    }

    @Test
    void readUtf8_shouldFail_whenAboveMaxSize() throws IOException {
        var result = ResponseBodies.readUtf8(ResponseBody.create("123456", JSON), 5);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).isEqualTo("The response body exceeds the maximum size of 5 bytes");
    }

    @Test
    void readUtf8_shouldReturnEmptyString_whenBodyIsEmpty() throws IOException {
        var result = ResponseBodies.readUtf8(ResponseBody.create("", JSON), 5);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEmpty();
    }
}
