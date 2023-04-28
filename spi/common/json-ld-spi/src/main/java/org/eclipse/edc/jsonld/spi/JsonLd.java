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
     * Compact a JsonLD document
     *
     * @param json the expanded json.
     * @return a successful {@link Result} containing the compacted {@link JsonObject} if the operation succeed, a failed one otherwise
     */
    Result<JsonObject> compact(JsonObject json);

    /**
     * Register a JsonLD context
     *
     * @param prefix the prefix
     * @param url the string representing the URL
     */
    void registerContext(String prefix, String url);

}
