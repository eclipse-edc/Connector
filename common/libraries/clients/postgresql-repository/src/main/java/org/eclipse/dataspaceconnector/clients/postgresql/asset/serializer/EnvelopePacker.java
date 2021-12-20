/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.types.Envelope;
import org.eclipse.dataspaceconnector.spi.EdcException;

public class EnvelopePacker {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Envelops the object and serializes it.
     *
     * @param obj object
     * @return serialized, enveloped object
     */
    public static String pack(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(new Envelope(obj, obj.getClass().getName()));
        } catch (JsonProcessingException e) {
            throw new EdcException(
                    String.format("PostgreSQL-Repository: Property value of type %s must be serializable.", obj.getClass()));
        }
    }

    /**
     * Deserializes the envelope and returns the enveloped object.
     *
     * @param serializedEnvelope serialized {@link Envelope}
     * @return unpacked object
     */
    public static <T> T unpack(String serializedEnvelope) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(serializedEnvelope);

            JsonNode className = jsonNode.get(Envelope.PROPERTY_NAME_CLASS_NAME);
            if (className == null) {
                return null;
            }

            Class<?> targetClass = Class.forName(className.asText());

            JsonNode obj = jsonNode.get(Envelope.PROPERTY_NAME_OBJ);
            if (obj == null) {
                return null;
            }

            //noinspection unchecked
            return (T) OBJECT_MAPPER.convertValue(obj, targetClass);
        } catch (JsonProcessingException e) {
            throw new EdcException("PostgreSQL-Repository: Cannot deserialize property value envelope", e);
        } catch (ClassNotFoundException e) {
            throw new EdcException("PostgreSQL-Repository: Enveloped class not found", e);
        }
    }
}
