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

package org.eclipse.edc.connector.api.management.secret.v1;

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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/secrets")
public class SecretsApiV1Controller extends BaseSecretsApiController implements SecretsApiV1 {
    private final Monitor monitor;

    public SecretsApiV1Controller(SecretService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        super(service, transformerRegistry, validator);
        this.monitor = monitor;
    }

    @POST
    @Override
    public JsonObject createSecretV1(JsonObject secretJson) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        return createSecret(secretJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getSecretV1(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        return getSecret(id);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeSecretV1(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        removeSecret(id);
    }

    @PUT
    @Override
    public void updateSecretV1(JsonObject secretJson) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        updateSecret(secretJson);
    }
}
