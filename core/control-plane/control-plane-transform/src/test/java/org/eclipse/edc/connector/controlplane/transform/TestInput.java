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

package org.eclipse.edc.connector.controlplane.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.CachedDocumentRegistry;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;

/**
 * Functions for shaping test input.
 */
public class TestInput {

    /**
     * Expands test input as Json-ld is required to be in this form
     */
    public static JsonObject getExpanded(JsonObject message) {
        var jsonLd = new TitaniumJsonLd(new ConsoleMonitor());
        CachedDocumentRegistry.getDocuments().forEach(result -> result
                .onSuccess(c -> jsonLd.registerCachedDocument(c.url(), c.resource()))
        );
        return jsonLd.expand(message).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }

    private TestInput() {
    }
}
