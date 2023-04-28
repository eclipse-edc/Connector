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

package org.eclipse.edc.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;

/**
 * Implementation of the {@link JsonLd} interface that uses the Titanium library for all JSON-LD operations.
 */
public class TitaniumJsonLd implements JsonLd {
    private final Monitor monitor;
    private final Map<String, String> additionalNamespaces;

    public TitaniumJsonLd(Monitor monitor) {
        this.monitor = monitor;
        additionalNamespaces = new HashMap<>();
    }

    @Override
    public Result<JsonObject> expand(JsonObject json) {
        try {
            var document = JsonDocument.of(json);
            var expanded = com.apicatalog.jsonld.JsonLd.expand(document).get();
            return Result.success(expanded.getJsonObject(0));
        } catch (JsonLdError error) {
            monitor.warning("Error expanding JSON-LD structure", error);
            return Result.failure(error.getMessage());
        }
    }

    @Override
    public Result<JsonObject> compact(JsonObject json) {
        try {
            var document = JsonDocument.of(json);
            var jsonFactory = Json.createBuilderFactory(Map.of());
            var contextDocument = JsonDocument.of(jsonFactory.createObjectBuilder()
                    .add(CONTEXT, createContextObject())
                    .build());
            var compacted = com.apicatalog.jsonld.JsonLd.compact(document, contextDocument).get();
            return Result.success(compacted);
        } catch (JsonLdError e) {
            monitor.warning("Error compacting JSON-LD structure", e);
            return Result.failure(e.getMessage());
        }
    }

    @Override
    public void registerNamespace(String prefix, String contextIri) {
        additionalNamespaces.put(prefix, contextIri);
    }

    private JsonObject createContextObject() {
        var builder = Json.createObjectBuilder();
        additionalNamespaces.forEach(builder::add);
        return builder.build();
    }
}
