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

package org.eclipse.edc.connector.dataplane.http.pipeline;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.eclipse.edc.connector.dataplane.http.testfixtures.HttpTestFixtures.formatRequestBodyAsString;

class HttpRequestParamsTests {
    private static final String SCHEME = "http";
    private static final String HOST = "some.base.url";
    private static final String BASE_URL = String.format("%s://%s", SCHEME, HOST);

    @ParameterizedTest
    @NullAndEmptySource
    void verifyPathIgnoredWhenNullOrBlank(String p) {
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.GET.name())
                .path(p)
                .build();

        var request = params.toRequest();

        assertBaseUrl(request.url().url());
    }

    @Test
    void verifyQueryParamsIgnoredWhenNullOrBlank() {
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.GET.name())
                .queryParams(Map.of())
                .build();

        var request = params.toRequest();

        assertBaseUrl(request.url().url());
    }

    @Test
    void verifyHeaders() {
        var headers = Map.of("key1", "value1");
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.GET.name())
                .headers(headers)
                .build();

        var request = params.toRequest();

        assertThat(request.headers()).isNotNull();
        headers.forEach((s, s2) -> assertThat(request.header(s)).isNotNull().isEqualTo(s2));
    }

    @Test
    void verifyComplexUrl() {
        var path = "testpath";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.GET.name())
                .path(path)
                .queryParam("foo", "bar")
                .queryParam("hello", "world")
                .build();

        var httpRequest = params.toRequest();

        var url = httpRequest.url().url();
        assertBaseUrl(url);
        assertThat(url.getPath()).isEqualTo("/" + path);
        assertThat(url.getQuery()).contains("foo=bar").contains("hello=world");
        assertThat(httpRequest.method()).isEqualTo(HttpMethod.GET.name());
    }

    @Test
    void verifyAggregatesQueryParamsAndPathFromBaseUrl() {
        var compositeBaseUrl = BASE_URL + "/basepath?foo=bar";
        var path = "testpath";
        var queryParams = Map.of("hello", "world");
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(compositeBaseUrl)
                .method(HttpMethod.GET.name())
                .path(path)
                .queryParams(queryParams)
                .build();

        var request = params.toRequest();

        var url = request.url().url();
        assertBaseUrl(url);
        assertThat(url.getPath()).isEqualTo("/basepath/" + path);
        assertThat(url.getQuery())
                .contains("foo=bar")
                .contains("hello=world");
        assertThat(request.method()).isEqualTo(HttpMethod.GET.name());
    }

    @Test
    void verifyDefaultContentTypeIsOctetStream() {
        var body = "Test body";
        var contentType = "application/octet-stream";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.POST.name())
                .body(body)
                .build();

        var request = params.toRequest();

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
                .method(HttpMethod.POST.name())
                .contentType(contentType)
                .body(body)
                .build();

        var request = params.toRequest();

        var requestBody = request.body();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.contentType()).hasToString(contentType);
        assertThat(formatRequestBodyAsString(requestBody)).isEqualTo(body);
    }


    @Test
    void verifyBodyFromMethodCall() throws IOException {
        var body = "Test body";
        var contentType = "application/json";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.POST.name())
                .contentType(contentType)
                .build();

        var request = params.toRequest(() -> new ByteArrayInputStream(body.getBytes()));

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
                .method(HttpMethod.GET.name())
                .contentType(contentType)
                .build();

        var request = params.toRequest();

        var requestBody = request.body();
        assertThat(requestBody).isNull();
    }

    @Test
    void verifyExceptionThrownIfBaseUrlMissing() {
        var builder = HttpRequestParams.Builder.newInstance().method(HttpMethod.GET.name());

        assertThatNullPointerException().isThrownBy(builder::build);
    }

    @Test
    void verifyExceptionThrownIfMethodMissing() {
        var builder = HttpRequestParams.Builder.newInstance().baseUrl(BASE_URL);

        assertThatNullPointerException().isThrownBy(builder::build);
    }

    @Test
    void verifyExceptionIsRaisedIfContentTypeIsNull() {
        var builder = HttpRequestParams.Builder.newInstance()
                .baseUrl(BASE_URL)
                .method(HttpMethod.POST.name())
                .contentType(null)
                .body("Test Body");

        assertThatNullPointerException().isThrownBy(builder::build);
    }

    private void assertBaseUrl(URL url) {
        assertThat(url.getProtocol()).isEqualTo(SCHEME);
        assertThat(url.getHost()).isEqualTo(HOST);
    }
}