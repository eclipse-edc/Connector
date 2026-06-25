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
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.InterceptorContext;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.util.function.Function;

@Provider
public class ProfileJerseyJsonLdInterceptor extends AbstractJerseyJsonLdInterceptor {

    private final String scopePrefix;
    private final Function<UriInfo, String> profileProvider;

    public ProfileJerseyJsonLdInterceptor(JsonLd jsonLd, TypeManager typeManager, String typeContext, String scopePrefix, Function<UriInfo, String> profileProvider) {
        this(jsonLd, typeManager, typeContext, scopePrefix, profileProvider, null, null);
    }

    public ProfileJerseyJsonLdInterceptor(JsonLd jsonLd, TypeManager typeManager, String typeContext, String scopePrefix, Function<UriInfo, String> profileProvider, JsonObjectValidatorRegistry validatorRegistry, String schemaVersion) {
        super(jsonLd, typeManager, typeContext, validatorRegistry, schemaVersion);
        this.profileProvider = profileProvider;
        this.scopePrefix = scopePrefix;
    }

    @Override
    protected JsonObject compact(JsonObject jsonObject, WriterInterceptorContext context) {
        var profileScope = resolveScope(context);
        // we cannot infer the profile scope at runtime, so we just return the original JSON document without compacting it
        if (profileScope != null) {
            return jsonLd.compact(jsonObject, profileScope)
                    .orElseThrow(f -> new InternalServerErrorException("Failed to compact JsonObject: " + f.getFailureDetail()));
        } else {
            return jsonObject;
        }
    }

    protected String resolveScope(InterceptorContext ctx) {
        var uriInfo = ctx.getProperty(UrlInfoRequestFilter.REQUEST_URL_INFO_PROPERTY);
        if (uriInfo instanceof UriInfo u) {
            var profile = profileProvider.apply(u);
            if (profile != null) {
                return scopePrefix + ":" + profile;
            }
            return null;
        } else {
            throw new EdcException("Request URL info not found in context");
        }
    }
}
