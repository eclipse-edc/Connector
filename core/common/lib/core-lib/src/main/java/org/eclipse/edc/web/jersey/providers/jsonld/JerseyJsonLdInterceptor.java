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

import jakarta.json.JsonObject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Provider
public class JerseyJsonLdInterceptor extends AbstractJerseyJsonLdInterceptor {

    private final String scope;

    public JerseyJsonLdInterceptor(JsonLd jsonLd, TypeManager typeManager, String typeContext, String scope) {
        this(jsonLd, typeManager, typeContext, scope, null, null);
    }

    public JerseyJsonLdInterceptor(JsonLd jsonLd, TypeManager typeManager, String typeContext, String scope, JsonObjectValidatorRegistry validatorRegistry, String schemaVersion) {
        super(jsonLd, typeManager, typeContext, validatorRegistry, schemaVersion);
        this.scope = scope;
    }

    @Override
    protected JsonObject compact(JsonObject jsonObject, WriterInterceptorContext context) {
        return jsonLd.compact(jsonObject, scope)
                .orElseThrow(f -> new InternalServerErrorException("Failed to compact JsonObject: " + f.getFailureDetail()));
    }
}
