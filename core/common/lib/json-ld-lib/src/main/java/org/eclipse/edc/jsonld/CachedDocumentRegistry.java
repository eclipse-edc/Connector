/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLdContext;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_ODRL_PROFILE_2025_1;
import static org.eclipse.edc.jsonld.spi.Namespaces.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;

/**
 * Manages the hardcoded json-ld documents cached
 */
public class CachedDocumentRegistry {

    /**
     * Get the cached documents
     *
     * @return the cached documents
     */
    public static Stream<Result<JsonLdContext>> getDocuments() {
        return Map.of(
                "odrl.jsonld", "http://www.w3.org/ns/odrl.jsonld",
                "dspace.jsonld", "https://w3id.org/dspace/2024/1/context.json",
                "management-context-v1.jsonld", EDC_CONNECTOR_MANAGEMENT_CONTEXT,
                "management-context-v2.jsonld", EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2,
                "dspace-edc-context-v1.jsonld", EDC_DSPACE_CONTEXT,
                "dspace-v2025-1.jsonld", DSPACE_CONTEXT_2025_1,
                "dspace-v2025-1-odrl.jsonld", DSPACE_ODRL_PROFILE_2025_1
        ).entrySet().stream()
                .map(entry -> getResourceUri("document/" + entry.getKey())
                .map(uri -> new JsonLdContext(uri, entry.getValue())));
    }

    static Result<URI> getResourceUri(String name) {
        var uri = CachedDocumentRegistry.class.getClassLoader().getResource(name);
        if (uri == null) {
            return Result.failure(format("Cannot find resource %s", name));
        }

        try {
            return Result.success(uri.toURI());
        } catch (URISyntaxException e) {
            return Result.failure(format("Cannot read resource %s: %s", name, e.getMessage()));
        }
    }

}
