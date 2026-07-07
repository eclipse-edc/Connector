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

package org.eclipse.edc.jsonld.cache;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.result.Result;

import java.io.StringReader;

/**
 * Helpers to parse the raw JSON content stored in a {@link org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext}.
 */
public final class JsonLdContexts {

    private JsonLdContexts() {
    }

    /**
     * Parses a raw JSON string into a {@link JsonObject}.
     */
    public static Result<JsonObject> toJsonObject(String content) {
        if (content == null) {
            return Result.failure("content is null");
        }
        try (var reader = Json.createReader(new StringReader(content))) {
            return Result.success(reader.readObject());
        } catch (Exception e) {
            return Result.failure("Cannot parse JSON-LD document: " + e.getMessage());
        }
    }
}
