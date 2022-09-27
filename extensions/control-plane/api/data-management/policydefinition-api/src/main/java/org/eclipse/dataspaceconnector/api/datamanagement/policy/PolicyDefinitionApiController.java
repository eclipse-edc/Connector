/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.api.model.IdResponseDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.api.ServiceResultHandler.mapToException;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/policydefinitions")
public class PolicyDefinitionApiController implements PolicyDefinitionApi {

    private final Monitor monitor;
    private final PolicyDefinitionService policyDefinitionService;
    private final DtoTransformerRegistry transformerRegistry;

    public PolicyDefinitionApiController(Monitor monitor, PolicyDefinitionService policyDefinitionService, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.policyDefinitionService = policyDefinitionService;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Override
    public List<PolicyDefinitionResponseDto> getAllPolicies(@Valid @BeanParam QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        monitor.debug(format("get all policies %s", spec));

        var queryResult = policyDefinitionService.query(spec);
        if (queryResult.failed()) {
            throw mapToException(queryResult, PolicyDefinition.class, null);
        }

        try (var stream = queryResult.getContent()) {
            return stream
                    .map(it -> transformerRegistry.transform(it, PolicyDefinitionResponseDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(Collectors.toList());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public PolicyDefinitionResponseDto getPolicy(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return policy with ID %s", id));
        return Optional.of(id)
                .map(it -> policyDefinitionService.findById(id))
                .map(it -> transformerRegistry.transform(it, PolicyDefinitionResponseDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(Policy.class, id));
    }

    @POST
    @Override
    public IdResponseDto createPolicy(PolicyDefinitionRequestDto requestDto) {
        var transformResult = transformerRegistry.transform(requestDto, PolicyDefinition.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException(transformResult.getFailureMessages());
        }

        var definition = transformResult.getContent();

        var result = policyDefinitionService.create(definition);
        if (result.succeeded()) {
            monitor.debug(format("Policy definition created %s", definition.getId()));
            var resultContent = result.getContent();
            return IdResponseDto.Builder.newInstance()
                    .id(resultContent.getId())
                    .createdAt(resultContent.getCreatedAt())
                    .build();
        } else {
            throw mapToException(result, PolicyDefinition.class, definition.getId());
        }
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deletePolicy(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete policy with id %s", id));
        var result = policyDefinitionService.deleteById(id);
        if (result.succeeded()) {
            monitor.debug(format("Policy deleted %s", id));
        } else {
            throw mapToException(result, PolicyDefinition.class, id);
        }
    }

}
