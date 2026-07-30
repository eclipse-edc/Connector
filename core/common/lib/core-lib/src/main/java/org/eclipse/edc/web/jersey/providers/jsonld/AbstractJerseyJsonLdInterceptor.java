/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.providers.jsonld;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.IOException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;

public abstract class AbstractJerseyJsonLdInterceptor implements WriterInterceptor {
    protected final JsonLd jsonLd;
    protected final TypeManager typeManager;
    protected final String typeContext;

    public AbstractJerseyJsonLdInterceptor(JsonLd jsonLd, TypeManager typeManager, String typeContext) {
        this.jsonLd = jsonLd;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (context.getEntity() instanceof JsonArray jsonArray) {
            var compacted = jsonArray.stream().map(it -> {
                if (it instanceof JsonObject jsonObject) {
                    return this.compact(jsonObject, context);
                } else {
                    return it;
                }
            }).collect(toJsonArray());

            context.setEntity(compacted);
        } else if (context.getEntity() instanceof JsonObject jsonObject) {
            context.setEntity(compact(jsonObject, context));
        }

        context.proceed();
    }

    protected abstract JsonObject compact(JsonObject jsonObject, WriterInterceptorContext context);

}
