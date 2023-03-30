/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.util;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.EdcException;

import java.util.Map;

public class JsonLdUtil {
    
    private JsonLdUtil() { }
    
    /**
     * Expands a JSON-LD structure. When expanding, the prefixes for attributes are resolved using
     * the context entries. and replaced by the respective context reference.
     *
     * @param object the structure to expand
     * @return the expanded structure in an array
     */
    public static JsonArray expand(JsonObject object) {
        try {
            var document = JsonDocument.of(object);
            return JsonLd.expand(document).get();
        } catch (JsonLdError e) {
            throw new EdcException("Failed to expand JSON-LD", e);
        }
    }
    
    /**
     * Compacts a JSON-LD structure. When compacting, the context references of attributes are
     * replaced by the respective context prefixes. A context object for resolving the prefixes
     * is added to the structure.
     *
     * @param object the structure to compact
     * @param context the context for compacting
     * @return the compacted structure
     */
    public static JsonObject compact(JsonObject object, JsonObject context) {
        try {
            var document = JsonDocument.of(object);
            var jsonFactory = Json.createBuilderFactory(Map.of());
            var contextDocument = JsonDocument.of(jsonFactory.createObjectBuilder()
                    .add("@context", context)
                    .build());
            return JsonLd.compact(document, contextDocument).get();
        } catch (JsonLdError e) {
            throw new EdcException("Failed to compact JSON-LD", e);
        }
    }
    
}
