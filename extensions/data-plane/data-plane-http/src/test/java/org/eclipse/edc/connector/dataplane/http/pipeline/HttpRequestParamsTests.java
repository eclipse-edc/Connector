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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.connector.dataplane.http.testfixtures.HttpTestFixtures.formatRequestBodyAsString;

class HttpRequestParamsTests {
    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://some.base.url";
    }

    @ParameterizedTest
    @NullAndEmptySource
    void verifyPathIgnoredWhenNullOrBlank(String p) {
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.GET.name())
                .path(p)
                .build();

        assertThat(params.toRequest().url().url()).hasToString(baseUrl + "/");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void verifyQueryParamsIgnoredWhenNullOrBlank(String qp) {
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.GET.name())
                .queryParams(qp)
                .build();

        assertThat(params.toRequest().url().url()).hasToString(baseUrl + "/");
    }

    @Test
    void verifyHeaders() {
        var headers = Map.of("key1", "value1");
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.GET.name())
                .headers(headers)
                .build();

        var httpRequest = params.toRequest();
        assertThat(httpRequest.headers()).isNotNull();
        headers.forEach((s, s2) -> assertThat(httpRequest.header(s)).isNotNull().isEqualTo(s2));
    }

    @Test
    void verifyComplexUrl() {
        var path = "testpath";
        var queryParams = "test-queryparams";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.GET.name())
                .path(path)
                .queryParams(queryParams)
                .build();

        var httpRequest = params.toRequest();

        assertThat(httpRequest.url().url()).hasToString(String.format("%s/%s?%s", baseUrl, path, queryParams));
        assertThat(httpRequest.method()).isEqualTo(HttpMethod.GET.name());
    }

    @Test
    void verifyDefaultContentTypeIsOctetStream() {
        var body = "Test body";
        var contentType = "application/octet-stream";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.POST.name())
                .body(body)
                .build();

        var httpRequest = params.toRequest();

        var requestBody = httpRequest.body();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.contentType()).hasToString(contentType);
    }

    @Test
    void verifyBodyFromParams() throws IOException {
        var body = "Test body";
        var contentType = "text/plain";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.POST.name())
                .contentType(contentType)
                .body(body)
                .build();

        var httpRequest = params.toRequest();

        var requestBody = httpRequest.body();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.contentType()).hasToString(contentType);
        assertThat(formatRequestBodyAsString(requestBody)).isEqualTo(body);
    }


    @Test
    void verifyBodyFromMethodCall() throws IOException {
        var body = "Test body";
        var contentType = "application/json";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.POST.name())
                .contentType(contentType)
                .build();

        var httpRequest = params.toRequest(() -> new ByteArrayInputStream(body.getBytes()));

        var requestBody = httpRequest.body();
        assertThat(requestBody).isNotNull();
        assertThat(requestBody.contentType()).hasToString(contentType);
        assertThat(formatRequestBodyAsString(requestBody)).isEqualTo(body);
    }

    @Test
    void verifyRequestBodyIsNullIfNoContentProvided() {
        var contentType = "test/content-type";
        var params = HttpRequestParams.Builder.newInstance()
                .baseUrl(baseUrl)
                .method(HttpMethod.GET.name())
                .contentType(contentType)
                .build();

        var httpRequest = params.toRequest();

        var requestBody = httpRequest.body();
        assertThat(requestBody).isNull();
    }

    @Test
    void verifyExceptionThrownIfBaseUrlMissing() {
        var builder = HttpRequestParams.Builder.newInstance().method(HttpMethod.GET.name());

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(builder::build);
    }

    @Test
    void verifyExceptionThrownIfMethodMissing() {
        var builder = HttpRequestParams.Builder.newInstance().baseUrl("http://some.base.url");

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(builder::build);
    }

    @Test
    void verifyExceptionIsRaisedIfContentTypeIsNull() {
        var builder = HttpRequestParams.Builder.newInstance()
                .baseUrl("http://some.base.url")
                .method(HttpMethod.POST.name())
                .contentType(null)
                .body("Test Body");

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(builder::build);
    }
}