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

package org.eclipse.edc.jsonld.cache.api.v5;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContextService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/jsonldcontexts")
public class JsonLdContextCacheApiV5Controller implements JsonLdContextCacheApiV5 {

    private final CachedJsonLdContextService service;
    private final TypeTransformerRegistry transformerRegistry;

    public JsonLdContextCacheApiV5Controller(CachedJsonLdContextService service, TypeTransformerRegistry transformerRegistry) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @RequiredScope("management-api:admin")
    @Override
    public JsonArray getAll() {
        return service.search(QuerySpec.max())
                .orElseThrow(exceptionMapper(CachedJsonLdContext.class))
                .stream()
                .map(this::toJson)
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{id}")
    @RequiredScope("management-api:admin")
    @Override
    public JsonObject get(@PathParam("id") String id) {
        var context = service.findById(id);
        if (context == null) {
            throw new ObjectNotFoundException(CachedJsonLdContext.class, id);
        }
        return toJson(context).orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @RequiredScope("management-api:admin")
    @Override
    public JsonObject create(@SchemaType(CACHED_JSON_LD_CONTEXT_TYPE_TERM) JsonObject request) {
        var context = fromJson(request);
        var created = service.create(context)
                .orElseThrow(exceptionMapper(CachedJsonLdContext.class, context.getUrl()));
        var responseDto = IdResponse.Builder.newInstance()
                .id(created.getId())
                .createdAt(created.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @PUT
    @Path("{id}")
    @RequiredScope("management-api:admin")
    @Override
    public void update(@PathParam("id") String id, @SchemaType(CACHED_JSON_LD_CONTEXT_TYPE_TERM) JsonObject request) {
        var context = fromJson(request).toBuilder().id(id).build();
        service.update(context)
                .orElseThrow(exceptionMapper(CachedJsonLdContext.class, id));
    }

    @DELETE
    @Path("{id}")
    @RequiredScope("management-api:admin")
    @Override
    public void delete(@PathParam("id") String id) {
        service.deleteById(id).orElseThrow(exceptionMapper(CachedJsonLdContext.class, id));
    }

    @POST
    @Path("{id}/refresh")
    @RequiredScope("management-api:admin")
    @Override
    public JsonObject refresh(@PathParam("id") String id) {
        var refreshed = service.refresh(id).orElseThrow(exceptionMapper(CachedJsonLdContext.class, id));
        return toJson(refreshed).orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    private CachedJsonLdContext fromJson(JsonObject request) {
        return transformerRegistry.transform(request, CachedJsonLdContext.class)
                .orElseThrow(InvalidRequestException::new);
    }

    private Result<JsonObject> toJson(CachedJsonLdContext context) {
        return transformerRegistry.transform(context, JsonObject.class);
    }
}
