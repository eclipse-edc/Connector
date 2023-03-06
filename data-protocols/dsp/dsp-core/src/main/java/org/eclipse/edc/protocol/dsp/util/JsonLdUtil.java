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

package org.eclipse.edc.protocol.dsp.util;

import java.net.URI;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.EdcException;

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
        var contextUri = URI.create("http://schema.org/"); //TODO context
        try {
            var document = JsonDocument.of(jsonObject);
            return JsonLd.compact(document, contextUri).get();
        } catch (JsonLdError e) {
            throw new EdcException(e);
        }
    }
    
}
