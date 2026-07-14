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

package org.eclipse.edc.validator.registration.api.v5;

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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistrationService;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration.SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/schemavalidators")
public class SchemaValidatorRegistrationApiV5Controller implements SchemaValidatorRegistrationApiV5 {

    private final SchemaValidatorRegistrationService service;
    private final TypeTransformerRegistry transformerRegistry;

    public SchemaValidatorRegistrationApiV5Controller(SchemaValidatorRegistrationService service, TypeTransformerRegistry transformerRegistry) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @RequiredScope("management-api:admin")
    @Override
    public JsonArray getAll() {
        return service.search(QuerySpec.max())
                .orElseThrow(exceptionMapper(SchemaValidatorRegistration.class))
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
        var registration = service.findById(id);
        if (registration == null) {
            throw new ObjectNotFoundException(SchemaValidatorRegistration.class, id);
        }
        return toJson(registration).orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @RequiredScope("management-api:admin")
    @Override
    public JsonObject create(@SchemaType(SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM) JsonObject request) {
        var registration = fromJson(request);
        var created = service.create(registration)
                .orElseThrow(exceptionMapper(SchemaValidatorRegistration.class, registration.getId()));
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
    public void update(@PathParam("id") String id, @SchemaType(SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM) JsonObject request) {
        var registration = fromJson(request).toBuilder().id(id).build();
        service.update(registration)
                .orElseThrow(exceptionMapper(SchemaValidatorRegistration.class, id));
    }

    @DELETE
    @Path("{id}")
    @RequiredScope("management-api:admin")
    @Override
    public void delete(@PathParam("id") String id) {
        service.deleteById(id).orElseThrow(exceptionMapper(SchemaValidatorRegistration.class, id));
    }

    private SchemaValidatorRegistration fromJson(JsonObject request) {
        return transformerRegistry.transform(request, SchemaValidatorRegistration.class)
                .orElseThrow(InvalidRequestException::new);
    }

    private Result<JsonObject> toJson(SchemaValidatorRegistration registration) {
        return transformerRegistry.transform(registration, JsonObject.class);
    }
}
