/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.web.jersey.providers.jsonld;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonObjectMessageBodyReader implements MessageBodyReader<JsonObject> {

    private final JsonLd jsonLd;
    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonObjectValidatorRegistry validatorRegistry;

    public JsonObjectMessageBodyReader(JsonLd jsonLd, TypeManager typeManager, String typeContext, JsonObjectValidatorRegistry validatorRegistry) {
        this.jsonLd = jsonLd;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.validatorRegistry = validatorRegistry;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        var bytes = entityStream.readAllBytes();
        if (bytes.length == 0) {
            return null;
        }

        var jsonObject = typeManager.getMapper(typeContext).readValue(bytes, JsonObject.class);

        var schemaType = Arrays.stream(annotations)
                .filter(a -> a.annotationType().equals(SchemaType.class))
                .map(a -> (SchemaType) a)
                .findFirst()
                .orElse(null);

        if (schemaType != null) {
            var objectType = jsonObject.getString(TYPE, null);
            if (objectType == null) {
                throw new InvalidRequestException("JsonObject is missing required property: " + TYPE);
            }
            if (!Arrays.asList(schemaType.value()).contains(objectType)) {
                throw new InvalidRequestException("JsonObject type '" + objectType + "' does not match expected types: " + Arrays.toString(schemaType.value()));
            }
            validatorRegistry.validate(schemaType.version() + ":" + objectType, jsonObject)
                    .orElseThrow(ValidationFailureException::new);
        }

        return jsonLd.expand(jsonObject)
                .orElseThrow(f -> new InvalidRequestException("Failed to expand JsonObject: " + f.getFailureDetail()));
    }
}
