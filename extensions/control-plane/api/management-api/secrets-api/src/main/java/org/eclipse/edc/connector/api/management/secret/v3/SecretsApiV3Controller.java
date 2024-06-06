/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.secret.v3;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.api.management.secret.BaseSecretsApiController;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/secrets")
public class SecretsApiV3Controller extends BaseSecretsApiController implements SecretsApiV3 {

    public SecretsApiV3Controller(SecretService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator) {
        super(service, transformerRegistry, validator);
    }

    @POST
    @Override
    public JsonObject createSecretV3(JsonObject secretJson) {
        return createSecret(secretJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getSecretV3(@PathParam("id") String id) {
        return getSecret(id);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeSecretV3(@PathParam("id") String id) {
        removeSecret(id);
    }

    @PUT
    @Override
    public void updateSecretV3(JsonObject secretJson) {
        updateSecret(secretJson);
    }
}
