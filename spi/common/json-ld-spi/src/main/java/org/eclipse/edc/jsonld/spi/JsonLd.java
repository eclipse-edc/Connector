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

import java.io.File;

/**
 * Provides JsonLD expansion/compaction functionalities.
 */
public interface JsonLd {

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
    Result<JsonObject> compact(JsonObject json);

    /**
     * Register a JsonLD namespace
     *
     * @param prefix the prefix
     * @param contextIri the string representing the IRI where the context is located
     */
    void registerNamespace(String prefix, String contextIri);

    /**
     * Register a JsonLD file document loader.
     * When an url is registered with a File, that url won't be called through http/https, but the content will be
     * loaded from the file parameter.
     *
     * @param url the url
     * @param file the file
     */
    void registerCachedDocument(String url, File file);

}
