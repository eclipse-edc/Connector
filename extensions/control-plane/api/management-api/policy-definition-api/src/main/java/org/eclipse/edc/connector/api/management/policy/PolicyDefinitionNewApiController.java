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

import jakarta.json.JsonObject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionRequestDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateWrapperDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/policydefinitions")
public class PolicyDefinitionNewApiController implements PolicyDefinitionNewApi {

    private final Monitor monitor;
    private final TypeTransformerRegistry transformerRegistry;
    private final PolicyDefinitionService service;
    private final JsonLd jsonLd;

    public PolicyDefinitionNewApiController(Monitor monitor, TypeTransformerRegistry transformerRegistry, PolicyDefinitionService service, JsonLd jsonLd) {
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.jsonLd = jsonLd;
    }

    @POST
    @Path("request")
    @Override
    public List<JsonObject> queryPolicyDefinitions(@Valid QuerySpecDto querySpecDto) {
        var querySpec = transformerRegistry.transform(querySpecDto, QuerySpec.class).orElseThrow(InvalidRequestException::new);
        try (var stream = service.query(querySpec).orElseThrow(exceptionMapper(PolicyDefinition.class))) {
            return stream
                    .map(policyDefinition -> transformerRegistry.transform(policyDefinition, PolicyDefinitionResponseDto.class)
                            .compose(dto -> transformerRegistry.transform(dto, JsonObject.class)
                            .compose(jsonLd::compact)))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toList());
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

        return transformerRegistry.transform(definition, PolicyDefinitionResponseDto.class)
                .compose(dto -> transformerRegistry.transform(dto, JsonObject.class))
                .compose(jsonLd::compact)
                .orElseThrow(failure -> new ObjectNotFoundException(PolicyDefinition.class, id));
    }

    @POST
    @Override
    public IdResponseDto createPolicyDefinition(JsonObject request) {
        var definition = jsonLd.expand(request)
                .compose(json -> transformerRegistry.transform(json, PolicyDefinitionRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, PolicyDefinition.class))
                .orElseThrow(InvalidRequestException::new);

        var createdDefinition = service.create(definition)
                .onSuccess(d -> monitor.debug(format("Policy Definition created %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, definition.getId()));

        return IdResponseDto.Builder.newInstance()
                .id(createdDefinition.getId())
                .createdAt(createdDefinition.getCreatedAt())
                .build();
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
        var policyDefinition = jsonLd.expand(input)
                .compose(expanded -> transformerRegistry.transform(expanded, PolicyDefinitionUpdateDto.class))
                .map(dto -> PolicyDefinitionUpdateWrapperDto.Builder.newInstance()
                        .policyId(id)
                        .updateRequest(dto)
                        .build())
                .compose(wrapper -> transformerRegistry.transform(wrapper, PolicyDefinition.class))
                .orElseThrow(InvalidRequestException::new);

        service.update(policyDefinition)
                .onSuccess(d -> monitor.debug(format("Policy Definition updated %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

}
