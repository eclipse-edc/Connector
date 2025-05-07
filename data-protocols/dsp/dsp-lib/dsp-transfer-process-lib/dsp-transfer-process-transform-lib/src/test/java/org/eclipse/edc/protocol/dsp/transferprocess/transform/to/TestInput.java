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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.to;

import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.JsonObject;

import static com.apicatalog.jsonld.JsonLd.expand;

/**
 * Functions for shaping test input.
 */
public class TestInput {

    /**
     * Expands test input as Json-ld is required to be in this form
     */
    public static JsonObject getExpanded(JsonObject message) {
        try {
            return expand(JsonDocument.of(message)).get().asJsonArray().getJsonObject(0);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private TestInput() {
    }
}
