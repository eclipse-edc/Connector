/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.api.management.secret.v4;

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
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/secrets")
public class SecretsApiV4Controller extends BaseSecretsApiController implements SecretsApiV4 {

    public SecretsApiV4Controller(SecretService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator) {
        super(service, transformerRegistry, validator);
    }

    @POST
    @Override
    public JsonObject createSecretV4(@SchemaType(EDC_SECRET_TYPE_TERM) JsonObject secretJson) {
        return createSecret(secretJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getSecretV4(@PathParam("id") String id) {
        return getSecret(id);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void removeSecretV4(@PathParam("id") String id) {
        removeSecret(id);
    }

    @PUT
    @Override
    public void updateSecretV4(@SchemaType(EDC_SECRET_TYPE_TERM) JsonObject secretJson) {
        updateSecret(secretJson);
    }
}
