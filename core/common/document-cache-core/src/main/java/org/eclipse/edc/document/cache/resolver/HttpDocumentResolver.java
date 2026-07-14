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

package org.eclipse.edc.document.cache.resolver;

import jakarta.json.JsonObject;
import okhttp3.Request;
import org.eclipse.edc.document.cache.spi.resolver.DocumentResolver;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;

import static org.eclipse.edc.document.cache.CachedDocuments.toJsonObject;

/**
 * Resolves a JSON-LD context document by fetching its {@code url} over http/https.
 */
public class HttpDocumentResolver implements DocumentResolver {

    private final EdcHttpClient httpClient;

    public HttpDocumentResolver(EdcHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Result<JsonObject> resolve(String url) {
        var request = new Request.Builder().url(url).get().build();
        return httpClient.execute(request, response -> {
            if (!response.isSuccessful()) {
                return Result.failure("Failed to fetch JSON-LD context '%s': HTTP %d".formatted(url, response.code()));
            }
            var body = response.body();
            if (body == null) {
                return Result.failure("Failed to fetch JSON-LD context '%s': empty response body".formatted(url));
            }
            try {
                return toJsonObject(body.string());
            } catch (IOException e) {
                return Result.failure("Failed to read JSON-LD context '%s': %s".formatted(url, e.getMessage()));
            }
        });
    }
}
