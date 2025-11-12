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

package org.eclipse.edc.connector.controlplane.api.management.policy.v4;

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
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4beta/policydefinitions")
public class PolicyDefinitionApiV4Controller extends BasePolicyDefinitionApiController implements PolicyDefinitionApiV4 {

    public PolicyDefinitionApiV4Controller(Monitor monitor, TypeTransformerRegistry transformerRegistry, PolicyDefinitionService service,
                                           JsonObjectValidatorRegistry validatorRegistry, SingleParticipantContextSupplier participantContextSupplier) {
        super(monitor, transformerRegistry, service, validatorRegistry, participantContextSupplier);
    }

    @POST
    @Path("request")
    @Override
    public JsonArray queryPolicyDefinitionsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return queryPolicyDefinitions(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getPolicyDefinitionV4(@PathParam("id") String id) {
        return getPolicyDefinition(id);
    }

    @POST
    @Override
    public JsonObject createPolicyDefinitionV4(@SchemaType(EDC_POLICY_DEFINITION_TYPE_TERM) JsonObject request) {
        return createPolicyDefinition(request);
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deletePolicyDefinitionV4(@PathParam("id") String id) {
        deletePolicyDefinition(id);
    }

    @PUT
    @Path("{id}")
    @Override
    public void updatePolicyDefinitionV4(@PathParam("id") String id, @SchemaType(EDC_POLICY_DEFINITION_TYPE_TERM) JsonObject input) {
        updatePolicyDefinition(id, input);
    }

    @POST
    @Path("{id}/validate")
    @Override
    public JsonObject validatePolicyDefinitionV4(@PathParam("id") String id) {
        return validatePolicyDefinition(id);
    }

    @POST
    @Path("{id}/evaluationplan")
    @Override
    public JsonObject createExecutionPlanV4(@PathParam("id") String id, @SchemaType(EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE_TERM) JsonObject request) {
        return createExecutionPlan(id, request);
    }
}
