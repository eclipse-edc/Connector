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

package org.eclipse.edc.connector.controlplane.api.management.policy.v2;

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
import org.eclipse.edc.connector.controlplane.api.management.policy.BasePolicyDefinitionApiController;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/policydefinitions")
public class PolicyDefinitionApiV2Controller extends BasePolicyDefinitionApiController implements PolicyDefinitionApiV2 {
    public PolicyDefinitionApiV2Controller(Monitor monitor, TypeTransformerRegistry transformerRegistry, PolicyDefinitionService service, JsonObjectValidatorRegistry validatorRegistry) {
        super(monitor, transformerRegistry, service, validatorRegistry);
    }

    @POST
    @Path("request")
    @Override
    public JsonArray queryPolicyDefinitionsV2(JsonObject querySpecJson) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return queryPolicyDefinitions(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getPolicyDefinitionV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getPolicyDefinition(id);
    }

    @POST
    @Override
    public JsonObject createPolicyDefinitionV2(JsonObject request) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return createPolicyDefinition(request);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deletePolicyDefinitionV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        deletePolicyDefinition(id);
    }

    @PUT
    @Path("{id}")
    @Override
    public void updatePolicyDefinitionV2(@PathParam("id") String id, JsonObject input) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        updatePolicyDefinition(id, input);
    }
}
