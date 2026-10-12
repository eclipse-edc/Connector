/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did.web.resolution;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.Mockito.mock;

class WebDidResolverTest {

    @Test
    void verifyResolveDocumentIsSuccessful() {
        var interceptor = new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                var didStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("did.json");
                assert didStream != null;
                var didDocument = new String(didStream.readAllBytes(), StandardCharsets.UTF_8);
                var body = ResponseBody.create(didDocument, MediaType.get("application/json"));
                return new Response.Builder().body(body).protocol(HTTP_1_1).request(chain.request()).code(200).message("ok").build();
            }
        };
        var resolver = createResolver(interceptor);

        var result = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");

        assertThat(result.getContent()).isNotNull();
    }

    @Test
    void verifyResolveDocumentNotFound() {
        var interceptor = new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Interceptor.Chain chain) {
                var body = ResponseBody.create("", MediaType.get("application/json"));
                return new Response.Builder().body(body).protocol(HTTP_1_1).request(chain.request()).code(404).message("notfound").build();
            }
        };
        var resolver = createResolver(interceptor);

        var result = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");

        assertThat(result.failed()).isTrue();
    }

    @Test
    void verifyResolve_whenServerErrorIsTransient_shouldRetry() {
        var calls = new AtomicInteger();
        var resolver = createResolver(chain -> calls.incrementAndGet() == 1 ? response(chain, 503, "") : response(chain, 200, didDocument()));

        var result = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");

        assertThat(result.succeeded()).isTrue();
        assertThat(calls).hasValue(2);
    }

    @Test
    void verifyResolve_whenServerErrorPersists_shouldFail() {
        var calls = new AtomicInteger();
        var resolver = createResolver(chain -> {
            calls.incrementAndGet();
            return response(chain, 503, "");
        });

        var result = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");

        assertThat(result.failed()).isTrue();
        // the test client's retry policy retries twice
        assertThat(calls).hasValue(3);
    }

    @Test
    void verifyResolve_whenClientError_shouldNotRetry() {
        var calls = new AtomicInteger();
        var resolver = createResolver(chain -> {
            calls.incrementAndGet();
            return response(chain, 404, "");
        });

        var result = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");

        assertThat(result.failed()).isTrue();
        assertThat(calls).hasValue(1);
    }

    private Response response(Interceptor.Chain chain, int code, String body) {
        return new Response.Builder().body(ResponseBody.create(body, MediaType.get("application/json"))).protocol(HTTP_1_1)
                .request(chain.request()).code(code).message("message").build();
    }

    private String didDocument() throws IOException {
        try (var didStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("did.json")) {
            assert didStream != null;
            return new String(didStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private WebDidResolver createResolver(Interceptor... interceptors) {
        return new WebDidResolver(testHttpClient(interceptors), true, new ObjectMapper(), mock(Monitor.class));
    }

}
