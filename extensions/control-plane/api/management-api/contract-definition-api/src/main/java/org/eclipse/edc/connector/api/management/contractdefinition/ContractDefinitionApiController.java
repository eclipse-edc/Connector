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

package org.eclipse.edc.connector.api.management.contractdefinition;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionCreateDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionUpdateDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionUpdateDtoWrapper;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/contractdefinitions")
public class ContractDefinitionApiController implements ContractDefinitionApi {
    private final Monitor monitor;
    private final ContractDefinitionService service;
    private final DtoTransformerRegistry transformerRegistry;

    public ContractDefinitionApiController(Monitor monitor, ContractDefinitionService service, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Override
    @Deprecated
    public List<ContractDefinitionResponseDto> getAllContractDefinitions(@Valid @BeanParam QuerySpecDto querySpecDto) {
        return queryContractDefinitions(querySpecDto);
    }

    @POST
    @Path("/request")
    @Override
    public List<ContractDefinitionResponseDto> queryAllContractDefinitions(QuerySpecDto querySpecDto) {
        return queryContractDefinitions(ofNullable(querySpecDto).orElse(QuerySpecDto.Builder.newInstance().build()));
    }

    @GET
    @Path("{id}")
    @Override
    public ContractDefinitionResponseDto getContractDefinition(@PathParam("id") String id) {
        monitor.debug(format("get contract definition with ID %s", id));

        return Optional.of(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, ContractDefinitionResponseDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @POST
    @Override
    public IdResponseDto createContractDefinition(@Valid ContractDefinitionCreateDto dto) {
        monitor.debug("Create new contract definition");
        var transformResult = transformerRegistry.transform(dto, ContractDefinition.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException(transformResult.getFailureMessages());
        }

        var contractDefinition = transformResult.getContent();

        var resultContent = service.create(contractDefinition).orElseThrow(exceptionMapper(ContractDefinition.class, dto.getId()));

        monitor.debug(format("Contract definition created %s", resultContent.getId()));
        return IdResponseDto.Builder.newInstance()
                .id(resultContent.getId())
                .createdAt(resultContent.getCreatedAt())
                .build();

    }

    @PUT
    @Path("{contractDefinitionId}")
    @Override
    public void updateContractDefinition(@PathParam("contractDefinitionId") String contractDefinitionId, @Valid ContractDefinitionUpdateDto contractDefinition) {
        var contractDefinitionWrapper = ContractDefinitionUpdateDtoWrapper.Builder
                .newInstance()
                .id(contractDefinitionId)
                .contractDefinition(contractDefinition)
                .build();

        var contractDefinitionResult = transformerRegistry.transform(contractDefinitionWrapper,
                ContractDefinition.class);
        if (contractDefinitionResult.failed()) {
            throw new InvalidRequestException(contractDefinitionResult.getFailureMessages());
        }
        service.update(contractDefinitionResult.getContent())
                .orElseThrow(exceptionMapper(ContractDefinition.class, contractDefinitionId));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deleteContractDefinition(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete contract definition with id %s", id));
        var result = service.delete(id).orElseThrow(exceptionMapper(ContractDefinition.class, id));
        monitor.debug(format("Contract definition deleted %s", result.getId()));
    }

    @NotNull
    private List<ContractDefinitionResponseDto> queryContractDefinitions(QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        monitor.debug(format("get all contract definitions %s", spec));

        try (var stream = service.query(spec).orElseThrow(exceptionMapper(ContractDefinition.class, null))) {
            return stream
                    .map(it -> transformerRegistry.transform(it, ContractDefinitionResponseDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(Collectors.toList());
        }


    }

}
