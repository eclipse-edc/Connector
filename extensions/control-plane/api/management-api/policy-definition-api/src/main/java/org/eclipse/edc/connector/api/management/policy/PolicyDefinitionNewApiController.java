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
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewRequestDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewResponseDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewUpdateDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewUpdateWrapperDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
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
    private final DtoTransformerRegistry transformerRegistry;
    private final PolicyDefinitionService service;

    public PolicyDefinitionNewApiController(Monitor monitor, DtoTransformerRegistry transformerRegistry, PolicyDefinitionService service) {
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
    }

    @POST
    @Path("request")
    @Override
    public List<PolicyDefinitionNewResponseDto> queryPolicyDefinitions(@Valid QuerySpecDto querySpecDto) {
        var querySpec = transformerRegistry.transform(querySpecDto, QuerySpec.class).orElseThrow(InvalidRequestException::new);
        try (var stream = service.query(querySpec).orElseThrow(exceptionMapper(PolicyDefinition.class))) {
            return stream
                    .map(policyDefinition -> transformerRegistry.transform(policyDefinition, PolicyDefinitionNewResponseDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toList());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public PolicyDefinitionNewResponseDto getPolicyDefinition(@PathParam("id") String id) {
        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        return transformerRegistry.transform(definition, PolicyDefinitionNewResponseDto.class)
                .orElseThrow(failure -> new ObjectNotFoundException(PolicyDefinition.class, id));
    }

    @POST
    @Override
    public IdResponseDto createPolicyDefinition(@Valid PolicyDefinitionNewRequestDto policy) {
        var inputDefinition = transformerRegistry.transform(policy, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        var createdDefinition = service.create(inputDefinition)
                .onSuccess(d -> monitor.debug(format("Policy Definition created %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, inputDefinition.getId()));

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
    public void updatePolicyDefinition(@PathParam("id") String id, @Valid PolicyDefinitionNewUpdateDto policy) {
        var wrapperDto = PolicyDefinitionNewUpdateWrapperDto.Builder.newInstance()
                .policyDefinitionId(id)
                .updateRequest(policy)
                .build();

        var policyDefinition = transformerRegistry.transform(wrapperDto, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(policyDefinition)
                .onSuccess(d -> monitor.debug(format("Policy Definition updated %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }
}
