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

package org.eclipse.edc.jsonld;

import java.util.Map;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.EdcException;

import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_SCHEMA;

public class JsonLdUtil {
    //TODO => JSON-LD extension
    private JsonLdUtil() { }
    
    public static JsonArray expandDocument(JsonObject jsonObject) {
        try {
            var document = JsonDocument.of(jsonObject);
            return JsonLd.expand(document).get();
        } catch (JsonLdError e) {
            throw new EdcException(e);
        }
    }
    
    public static JsonObject compactDocument(JsonObject jsonObject) {
        try {
            var document = JsonDocument.of(jsonObject);
    
            var builderFactory = Json.createBuilderFactory(Map.of());
            var contextObject = builderFactory
                    .createObjectBuilder()
                    .add(DCAT_PREFIX, DCAT_SCHEMA)
                    .add(ODRL_PREFIX, ODRL_SCHEMA)
                    .add(DCT_PREFIX, DCT_SCHEMA)
                    .build();
            var contextDocument = JsonDocument.of(builderFactory.createObjectBuilder()
                    .add("@context", contextObject)
                    .build());
            
            return JsonLd.compact(document, contextDocument).get();
        } catch (JsonLdError e) {
            throw new EdcException(e);
        }
    }
    
}
