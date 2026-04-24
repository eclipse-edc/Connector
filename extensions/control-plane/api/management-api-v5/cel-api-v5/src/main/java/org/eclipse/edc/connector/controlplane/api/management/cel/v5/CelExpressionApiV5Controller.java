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

package org.eclipse.edc.connector.controlplane.api.management.cel.v5;


import jakarta.annotation.security.RolesAllowed;
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
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.model.CelExpressionTestRequest;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_TYPE_TERM;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/celexpressions")
public class CelExpressionApiV5Controller implements CelExpressionApiV5 {

    private final CelPolicyExpressionService service;
    private final TypeTransformerRegistry transformerRegistry;

    public CelExpressionApiV5Controller(CelPolicyExpressionService service,
                                        TypeTransformerRegistry transformerRegistry) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject createExpressionV5(@SchemaType(CEL_EXPRESSION_TYPE_TERM) JsonObject expression) {

        var expr = transformerRegistry.transform(expression, CelExpression.class)
                .orElseThrow(InvalidRequestException::new);

        service.create(expr)
                .orElseThrow(exceptionMapper(CelExpression.class, expr.getId()));

        var responseDto = IdResponse.Builder.newInstance()
                .id(expr.getId())
                .createdAt(expr.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("test")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject testExpressionV5(@SchemaType(CEL_EXPRESSION_TEST_REQUEST_TYPE_TERM) JsonObject test) {

        var expr = transformerRegistry.transform(test, CelExpressionTestRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var response = service.test(expr)
                .orElseThrow(exceptionMapper(CelExpression.class));


        return transformerRegistry.transform(response, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @GET
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getExpressionV5(@PathParam("id") String id) {

        var expr = service.findById(id)
                .orElseThrow(exceptionMapper(ParticipantContext.class, id));

        return transformerRegistry.transform(expr, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @PUT
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public void updateExpressionV5(@PathParam("id") String id, @SchemaType(CEL_EXPRESSION_TYPE_TERM) JsonObject expression) {

        var expr = transformerRegistry.transform(expression, CelExpression.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(expr)
                .orElseThrow(exceptionMapper(CelExpression.class, id));


    }

    @POST
    @Path("request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray queryExpressionV5(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.query(querySpec).orElseThrow(exceptionMapper(CelExpression.class)).stream()
                .map(expression -> transformerRegistry.transform(expression, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public void deleteExpressionV5(@PathParam("id") String id) {
        service.delete(id)
                .orElseThrow(exceptionMapper(ParticipantContext.class, id));
    }
}
