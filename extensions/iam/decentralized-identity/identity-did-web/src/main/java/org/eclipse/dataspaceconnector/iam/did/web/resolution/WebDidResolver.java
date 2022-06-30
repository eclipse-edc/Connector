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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.web.resolution.DidFunctions.resolveDidDocumentUrl;

/**
 * Resolves a Web DID according to the Web DID specification (https://w3c-ccg.github.io/did-method-web).
 */
public class WebDidResolver implements DidResolver {
    private static final String DID_METHOD = "web";

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;
    private final boolean useHttpsScheme;

    /**
     * Creates a resolver that executes standard DNS lookups.
     */
    public WebDidResolver(OkHttpClient httpClient, boolean useHttpsScheme, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.useHttpsScheme = useHttpsScheme;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public @NotNull String getMethod() {
        return DID_METHOD;
    }

    @Override
    @NotNull
    public Result<DidDocument> resolve(String didKey) {
        try {
            var request = new Request.Builder().url(resolveDidDocumentUrl(didKey, useHttpsScheme)).get().build();

            try (var response = httpClient.newCall(request).execute()) {
                if (response.code() != 200) {
                    return Result.failure(format("Error resolving DID: %s. HTTP Code was: %s", didKey, response.code()));
                }
                try (var body = response.body()) {
                    if (body == null) {
                        return Result.failure("DID response contained an empty body: " + didKey);
                    }
                    DidDocument didDocument = mapper.readValue(body.string(), DidDocument.class);
                    return Result.success(didDocument);
                }
            } catch (IOException e) {
                monitor.severe("Error resolving DID: " + didKey, e);
                return Result.failure("Error resolving DID: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            monitor.severe("Invalid DID key: " + didKey, e);
            return Result.failure("Invalid DID key: " + e.getMessage());
        }
    }


}
