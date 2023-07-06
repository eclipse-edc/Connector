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

package org.eclipse.edc.connector.api.management.policy;

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
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static org.eclipse.edc.api.model.QuerySpecDto.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/policydefinitions")
public class PolicyDefinitionApiController implements PolicyDefinitionApi {

    private final Monitor monitor;
    private final TypeTransformerRegistry transformerRegistry;
    private final PolicyDefinitionService service;
    private final JsonObjectValidatorRegistry validatorRegistry;

    public PolicyDefinitionApiController(Monitor monitor, TypeTransformerRegistry transformerRegistry,
                                         PolicyDefinitionService service, JsonObjectValidatorRegistry validatorRegistry) {
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.validatorRegistry = validatorRegistry;
    }

    @POST
    @Path("request")
    @Override
    public JsonArray queryPolicyDefinitions(JsonObject querySpecDto) {
        QuerySpec querySpec;
        if (querySpecDto == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validatorRegistry.validate(EDC_QUERY_SPEC_TYPE, querySpecDto).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecDto, QuerySpecDto.class)
                    .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class))
                    .orElseThrow(InvalidRequestException::new);
        }

        try (var stream = service.query(querySpec).orElseThrow(exceptionMapper(PolicyDefinition.class))) {
            return stream
                    .map(policyDefinition -> transformerRegistry.transform(policyDefinition, JsonObject.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toJsonArray());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getPolicyDefinition(@PathParam("id") String id) {
        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        return transformerRegistry.transform(definition, JsonObject.class)
                .orElseThrow(failure -> new ObjectNotFoundException(PolicyDefinition.class, id));
    }

    @POST
    @Override
    public JsonObject createPolicyDefinition(JsonObject request) {
        validatorRegistry.validate(EDC_POLICY_DEFINITION_TYPE, request).orElseThrow(ValidationFailureException::new);

        var definition = transformerRegistry.transform(request, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        var createdDefinition = service.create(definition)
                .onSuccess(d -> monitor.debug(format("Policy Definition created %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, definition.getId()));

        var responseDto = IdResponseDto.Builder.newInstance()
                .id(createdDefinition.getId())
                .createdAt(createdDefinition.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deletePolicyDefinition(@PathParam("id") String id) {
        service.deleteById(id)
                .onSuccess(d -> monitor.debug(format("Policy Definition deleted %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

    @PUT
    @Path("{id}")
    @Override
    public void updatePolicyDefinition(@PathParam("id") String id, JsonObject input) {
        validatorRegistry.validate(EDC_POLICY_DEFINITION_TYPE, input).orElseThrow(ValidationFailureException::new);

        var policyDefinition = transformerRegistry.transform(input, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(policyDefinition)
                .onSuccess(d -> monitor.debug(format("Policy Definition updated %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

}
