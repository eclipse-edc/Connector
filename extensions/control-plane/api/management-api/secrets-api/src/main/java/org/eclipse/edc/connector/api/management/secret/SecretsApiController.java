/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Optional.of;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/secrets")
public class SecretsApiController implements SecretsApi {
    private final TypeTransformerRegistry transformerRegistry;
    private final SecretService service;
    private final JsonObjectValidatorRegistry validator;

    public SecretsApiController(SecretService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.validator = validator;
    }

    @POST
    @Override
    public JsonObject createSecret(JsonObject secretJson) {
        validator.validate(EDC_SECRET_TYPE, secretJson).orElseThrow(ValidationFailureException::new);

        var secret = transformerRegistry.transform(secretJson, Secret.class)
                .orElseThrow(InvalidRequestException::new);

        var idResponse = service.create(secret)
                .map(a -> IdResponse.Builder.newInstance()
                        .id(a.getId())
                        .createdAt(a.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(Secret.class, secret.getId()));

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getSecret(@PathParam("id") String id) {
        var secret = of(id)
                .map(it -> service.findById(id))
                .orElseThrow(() -> new ObjectNotFoundException(Secret.class, id));

        return transformerRegistry.transform(secret, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeSecret(@PathParam("id") String id) {
        service.delete(id).orElseThrow(exceptionMapper(Secret.class, id));
    }

    @PUT
    @Override
    public void updateSecret(JsonObject secretJson) {
        validator.validate(EDC_SECRET_TYPE, secretJson).orElseThrow(ValidationFailureException::new);

        var secretResult = transformerRegistry.transform(secretJson, Secret.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(secretResult)
                .orElseThrow(exceptionMapper(Secret.class, secretResult.getId()));
    }
}
