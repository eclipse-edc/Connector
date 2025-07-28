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

package org.eclipse.edc.connector.dataplane.http.params;

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.http.testfixtures.TestFunctions.formatRequestBodyAsString;

class HttpRequestFactoryTest {

    private static final String SCHEME = "http";
    private static final String HOST = "some.base.url";
    private static final String BASE_URL = String.format("%s://%s", SCHEME, HOST);

    private final HttpRequestFactory paramsToRequest = new HttpRequestFactory();

    @Nested
    class SourceSide {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"\\", "/"})
        void verifyPathIgnoredWhenNullOrBlank(String p) {
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("GET")
                    .path(p)
                    .build();

            var request = paramsToRequest.toRequest(params);

            assertBaseUrl(request.url().url());
        }

        @ParameterizedTest
        @NullAndEmptySource
        void verifyQueryParamsIgnoredWhenNullOrBlank(String qp) {
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("GET")
                    .queryParams(qp)
                    .build();

            var request = paramsToRequest.toRequest(params);

            assertBaseUrl(request.url().url());
        }

        @Test
        void verifyHeaders() {
            var headers = Map.of("key1", "value1");
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("GET")
                    .headers(headers)
                    .build();

            var request = paramsToRequest.toRequest(params);

            assertThat(request.headers()).isNotNull();
            headers.forEach((s, s2) -> assertThat(request.header(s)).isNotNull().isEqualTo(s2));
        }

        @Test
        void verifyComplexUrl() {
            var path = "testpath";
            var queryParams = "test-queryparams";
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("GET")
                    .path(path)
                    .queryParams(queryParams)
                    .build();

            var httpRequest = paramsToRequest.toRequest(params);

            var url = httpRequest.url().url();
            assertBaseUrl(url);
            assertThat(url.getPath()).isEqualTo("/" + path);
            assertThat(url.getQuery()).isEqualTo(queryParams);
            assertThat(httpRequest.method()).isEqualTo("GET");
        }

        @Test
        void verifyDefaultContentTypeIsOctetStream() {
            var body = "Test body";
            var contentType = "application/octet-stream";
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("POST")
                    .body(body)
                    .build();

            var request = paramsToRequest.toRequest(params);

            var requestBody = request.body();
            assertThat(requestBody).isNotNull();
            assertThat(requestBody.contentType()).hasToString(contentType);
        }

        @Test
        void verifyBodyFromParams() throws IOException {
            var body = "Test body";
            var contentType = "text/plain";
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("POST")
                    .contentType(contentType)
                    .body(body)
                    .build();

            var request = paramsToRequest.toRequest(params);

            var requestBody = request.body();
            assertThat(requestBody).isNotNull();
            assertThat(requestBody.contentType()).hasToString(contentType);
            assertThat(formatRequestBodyAsString(requestBody)).isEqualTo(body);
        }

        @Test
        void verifyRequestBodyIsNullIfNoContentProvided() {
            var contentType = "test/content-type";
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("GET")
                    .contentType(contentType)
                    .build();

            var request = paramsToRequest.toRequest(params);

            var requestBody = request.body();
            assertThat(requestBody).isNull();
        }

        private void assertBaseUrl(URL url) {
            assertThat(url.getProtocol()).isEqualTo(SCHEME);
            assertThat(url.getHost()).isEqualTo(HOST);
        }

    }

    @Nested
    class SinkSide {

        @Test
        void verifyBodyFromMethodCall() throws IOException {
            var body = "Test body";
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("POST")
                    .contentType("application/json")
                    .build();

            var request = paramsToRequest.toRequest(params, new TestPart("application/xml", body));

            var requestBody = request.body();
            assertThat(requestBody).isNotNull();
            assertThat(requestBody.contentType()).hasToString("application/xml");
            assertThat(formatRequestBodyAsString(requestBody)).isEqualTo(body);
        }

        @Test
        void verifyChunkedRequest() throws IOException {
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("POST")
                    .nonChunkedTransfer(false)
                    .build();

            var request = paramsToRequest.toRequest(params, new TestPart("application/octet-stream", "a body"));

            var body = request.body();
            assertThat(body).isNotNull();
            assertThat(body.contentLength()).isEqualTo(-1);
        }

        @Test
        void verifyNotChunkedRequest() throws IOException {
            var params = HttpRequestParams.Builder.newInstance()
                    .baseUrl(BASE_URL)
                    .method("POST")
                    .nonChunkedTransfer(true)
                    .build();

            var request = paramsToRequest.toRequest(params, new TestPart("application/octet-stream", "a body"));

            var body = request.body();
            assertThat(body).isNotNull();
            assertThat(body.contentLength()).isEqualTo(6);
        }

        private record TestPart(String mediaType, String data) implements DataSource.Part {

            @Override
            public String name() {
                return "test";
            }

            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(data.getBytes());
            }

            @Override
            public String mediaType() {
                return mediaType;
            }
        }
    }
}
