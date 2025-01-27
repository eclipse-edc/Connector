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

package org.eclipse.edc.web.jersey.providers.jsonld;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;

@Provider
public class JerseyJsonLdInterceptor implements ReaderInterceptor, WriterInterceptor {
    private final JsonLd jsonLd;
    private final TypeManager typeManager;
    private final String typeContext;
    private final String scope;

    public JerseyJsonLdInterceptor(JsonLd jsonLd, TypeManager typeManager, String typeContext, String scope) {
        this.jsonLd = jsonLd;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.scope = scope;
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        if (context.getType().equals(JsonObject.class)) {
            var bytes = context.getInputStream().readAllBytes();
            if (bytes.length > 0) {
                var jsonObject = typeManager.getMapper(typeContext).readValue(bytes, JsonObject.class);

                var expanded = jsonLd.expand(jsonObject)
                        .orElseThrow(f -> new BadRequestException("Failed to expand JsonObject: " + f.getFailureDetail()));

                var expandedBytes = typeManager.getMapper(typeContext).writeValueAsBytes(expanded);
                context.setInputStream(new ByteArrayInputStream(expandedBytes));
            }
        }

        return context.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (context.getEntity() instanceof JsonArray jsonArray) {
            var compacted = jsonArray.stream().map(it -> {
                if (it instanceof JsonObject jsonObject) {
                    return this.compact(jsonObject);
                } else {
                    return it;
                }
            }).collect(toJsonArray());

            context.setEntity(compacted);
        } else if (context.getEntity() instanceof JsonObject jsonObject) {
            context.setEntity(compact(jsonObject));
        }

        context.proceed();
    }

    private JsonObject compact(JsonObject jsonObject) {
        return jsonLd.compact(jsonObject, scope)
                .orElseThrow(f -> new InternalServerErrorException("Failed to compact JsonObject: " + f.getFailureDetail()));
    }
}
