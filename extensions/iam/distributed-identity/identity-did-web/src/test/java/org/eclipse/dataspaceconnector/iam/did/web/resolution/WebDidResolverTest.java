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
package org.eclipse.dataspaceconnector.iam.did.web.resolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;

/**
 * Verifies Web DID resolution.
 */
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
        var document = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");
        assertThat(document).isNotNull();
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
        var document = resolver.resolve("did:web:foo.com:edc:EiDfkaPHt8Yojnh15O7egrj5pA9tTefh_SYtbhF1-XyAeA");
        assertThat(document).isNull();
    }

    @Test
    void verifyConvertToUrl() throws Exception {
        var resolver = createResolver();

        assertThat(resolver.keyToUrl("did:web:w3c-ccg.github.io")).isEqualTo("https://w3c-ccg.github.io/.well-known/did.json");
        assertThat(resolver.keyToUrl("did:web:w3c-ccg.github.io:user:alice")).isEqualTo("https://w3c-ccg.github.io/user/alice/did.json");

        try {
            resolver.keyToUrl("did:web:w3c-ccg.github.io:user:alice:");
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            /// expected
        }
    }

    private WebDidResolver createResolver(Interceptor... interceptors) {
        var builder = new OkHttpClient.Builder();
        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }
        var mapper = new ObjectMapper();
        Monitor monitor = EasyMock.niceMock(Monitor.class);
        return new WebDidResolver(builder.build(), mapper, monitor);
    }


}
