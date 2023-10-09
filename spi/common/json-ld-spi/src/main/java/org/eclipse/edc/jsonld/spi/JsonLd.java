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

package org.eclipse.edc.jsonld.spi;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;

/**
 * Provides JsonLD expansion/compaction functionalities.
 */
public interface JsonLd {
    String DEFAULT_SCOPE = "*";

    /**
     * Expand a JsonLD document
     *
     * @param json the compacted json.
     * @return a successful {@link Result} containing the expanded {@link JsonObject} if the operation succeed, a failed one otherwise
     */
    Result<JsonObject> expand(JsonObject json);

    /**
     * Compact a JsonLD document. The context will be generated from registered contexts and namespaces.
     *
     * @param json the expanded json.
     * @return a successful {@link Result} containing the compacted {@link JsonObject} if the operation succeed, a failed one otherwise
     */
    default Result<JsonObject> compact(JsonObject json) {
        return compact(json, DEFAULT_SCOPE);
    }

    /**
     * Compact a JsonLD document. The context will be generated from registered contexts and namespaces.
     *
     * @param json  the expanded json.
     * @param scope the scope to apply during the compaction process
     * @return a successful {@link Result} containing the compacted {@link JsonObject} if the operation succeed, a failed one otherwise
     */
    Result<JsonObject> compact(JsonObject json, String scope);

    /**
     * Register a JsonLD namespace in the default scope
     *
     * @param prefix     the prefix
     * @param contextIri the string representing the IRI where the context is located
     */
    default void registerNamespace(String prefix, String contextIri) {
        registerNamespace(prefix, contextIri, DEFAULT_SCOPE);
    }

    /**
     * Register a JsonLD namespace in a provided scope
     *
     * @param prefix     the prefix
     * @param contextIri the string representing the IRI where the context is located
     * @param scope      the scope where the prefix will be applied
     */
    void registerNamespace(String prefix, String contextIri, String scope);

    /**
     * Register a JsonLD context URL in the default scope
     *
     * @param contextIri the string representing the IRI where the context is located
     */
    default void registerContext(String contextIri) {
        registerContext(contextIri, DEFAULT_SCOPE);
    }

    /**
     * Register a JsonLD context URL in the provided scope
     *
     * @param contextIri the string representing the IRI where the context is located
     */
    void registerContext(String contextIri, String scope);


    /**
     * Register a JsonLD file document loader.
     * When a document is cached, that url won't be called through http/https, but the content will be
     * loaded from the URI parameter.
     *
     * @param url the url
     * @param uri the file
     */
    void registerCachedDocument(String url, URI uri);

}
