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

package org.eclipse.edc.protocol.dsp.transferprocess.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.EdcException;

import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;

/**
 * Provides utility functions for HTTP delegates.
 */
public class DelegateUtil {
    
    //TODO use also in other dsp dispatcher modules: move to http-core or make Delegate abstract class
    
    private DelegateUtil() { }
    
    /**
     * Compacts a JSON-LD structure using the given context and serializes it.
     *
     * @param input the JSON-LD structure in expanded form
     * @param jsonLdContext the JSON-LD context to use for compaction
     * @param mapper the object mapper for serialization
     * @return the compacted JSON-LD serialized
     */
    public static String toCompactedJson(JsonObject input, JsonObject jsonLdContext, ObjectMapper mapper) {
        try {
            var content = compact(input, jsonLdContext);
            return mapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize JSON-LD.", e);
        }
    }
    
}
