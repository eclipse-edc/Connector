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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionId;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.api.ServiceResultHandler.mapToException;

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
    public List<ContractDefinitionResponseDto> getAllContractDefinitions(@Valid @BeanParam QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        monitor.debug(format("get all contract definitions %s", spec));

        var queryResult = service.query(spec);
        if (queryResult.failed()) {
            throw mapToException(queryResult, ContractDefinition.class, null);
        }

        return queryResult.getContent()
                .stream()
                .map(it -> transformerRegistry.transform(it, ContractDefinitionResponseDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
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
    public ContractDefinitionId createContractDefinition(@Valid ContractDefinitionRequestDto dto) {
        monitor.debug("Create new contract definition");
        var transformResult = transformerRegistry.transform(dto, ContractDefinition.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException(transformResult.getFailureMessages());
        }

        var contractDefinition = transformResult.getContent();

        var result = service.create(contractDefinition);
        if (result.succeeded()) {
            monitor.debug(format("Contract definition created %s", result.getContent().getId()));
            return new ContractDefinitionId(result.getContent().getId());
        } else {
            throw new ObjectExistsException(ContractDefinition.class, dto.getId());
        }
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deleteContractDefinition(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete contract definition with id %s", id));
        var result = service.delete(id);
        if (result.succeeded()) {
            monitor.debug(format("Contract definition deleted %s", result.getContent().getId()));
        } else {
            throw mapToException(result, ContractDefinition.class, id);
        }
    }

}
