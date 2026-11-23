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

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.client.ResponseBodies;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

/**
 * Resolves a Web DID according to the Web DID specification (https://w3c-ccg.github.io/did-method-web).
 */
public class WebDidResolver implements DidResolver {
    public static final String DEFAULT_CALL_TIMEOUT = "PT5S";
    public static final int DEFAULT_MAX_RETRIES = 1;
    public static final long DEFAULT_MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final String DID_METHOD = "web";

    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;
    private final WebDidUrlResolver urlResolver;
    private final long maxResponseBytes;

    /**
     * Creates a resolver that executes standard DNS lookups, accepting DID documents up to
     * {@link #DEFAULT_MAX_RESPONSE_BYTES}.
     */
    public WebDidResolver(EdcHttpClient httpClient, boolean useHttpsScheme, ObjectMapper mapper, Monitor monitor) {
        this(httpClient, useHttpsScheme, mapper, monitor, DEFAULT_MAX_RESPONSE_BYTES);
    }

    /**
     * Creates a resolver that executes standard DNS lookups.
     *
     * @param httpClient       the http client, its timeouts and retries apply to the DID document requests.
     * @param useHttpsScheme   whether the DID document URL uses {@code https}, otherwise {@code http}.
     * @param mapper           the object mapper.
     * @param monitor          the monitor.
     * @param maxResponseBytes the maximum size of a DID document, in bytes.
     */
    public WebDidResolver(EdcHttpClient httpClient, boolean useHttpsScheme, ObjectMapper mapper, Monitor monitor, long maxResponseBytes) {
        this.httpClient = httpClient;
        this.urlResolver = new WebDidUrlResolver(useHttpsScheme);
        this.mapper = mapper;
        this.monitor = monitor;
        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public @NotNull String getMethod() {
        return DID_METHOD;
    }

    @Override
    @NotNull
    public Result<DidDocument> resolve(String didKey) {
        String url;
        try {
            url = urlResolver.apply(didKey);
        } catch (IllegalArgumentException e) {
            monitor.severe("Invalid DID key: " + didKey, e);
            return Result.failure("Invalid DID key: " + e.getMessage());
        }

        var request = new Request.Builder().url(url).get().build();
        // a server error may be gone with the next attempt, so it is retried like a failed connection. The result is a
        // failure once the retries are exhausted.
        return httpClient.execute(request, List.of(retryWhenStatusNot2xxOr4xx()), response -> toDidDocument(didKey, response));
    }

    private Result<DidDocument> toDidDocument(String didKey, Response response) {
        if (response.code() != 200) {
            return Result.failure(format("Error resolving DID: %s. HTTP Code was: %s", didKey, response.code()));
        }
        try (var body = response.body()) {
            if (body == null) {
                return Result.failure("DID response contained an empty body: " + didKey);
            }
            var content = ResponseBodies.readUtf8(body, maxResponseBytes);
            if (content.failed()) {
                return Result.failure("Error resolving DID %s: %s".formatted(didKey, content.getFailureDetail()));
            }
            var didDocument = mapper.readValue(content.getContent(), DidDocument.class);
            return Result.success(didDocument);
        } catch (IOException e) {
            // the body could not be read (e.g. the call timed out), the DID may be resolved later
            monitor.severe("Error resolving DID: " + didKey, e);
            return Result.retryableFailure("Error resolving DID: " + e.getMessage());
        }
    }
}
