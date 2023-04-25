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

package org.eclipse.edc.protocol.dsp.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;

/**
 * Serializes {@link RemoteMessage}s to JSON-LD.
 */
public class JsonLdRemoteMessageSerializerImpl implements JsonLdRemoteMessageSerializer {
    
    private JsonLdTransformerRegistry registry;
    private ObjectMapper mapper;
    
    public JsonLdRemoteMessageSerializerImpl(JsonLdTransformerRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }
    
    /**
     * Serializes a {@link RemoteMessage} to JSON-LD. The message is first transformed using the
     * {@link JsonLdTransformerRegistry}, then the resulting JSON-LD structure is compacted using
     * the given JSON-LD context before returning it as a string.
     *
     * @param message the message to serialize
     * @param jsonLdContext the JSON-LD context
     * @return the serialized message
     */
    @Override
    public String serialize(RemoteMessage message, JsonObject jsonLdContext) {
        try {
            var transformResult = registry.transform(message, JsonObject.class);
            if (transformResult.succeeded()) {
                var compacted = compact(transformResult.getContent(), jsonLdContext);
                return mapper.writeValueAsString(compacted);
            }
            throw new EdcException(format("Failed to transform %s: %s", message.getClass().getSimpleName(), join(", ", transformResult.getFailureMessages())));
        } catch (JsonProcessingException e) {
            throw new EdcException(format("Failed to serialize %s", message.getClass().getSimpleName()), e);
        }
    }
}
