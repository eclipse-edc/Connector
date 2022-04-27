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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service.ContractDefinitionService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
    @Produces({ MediaType.APPLICATION_JSON })
    @Override
    public List<ContractDefinitionDto> getAllContractDefinitions(@QueryParam("offset") Integer offset,
                                                                 @QueryParam("limit") Integer limit,
                                                                 @QueryParam("filter") String filterExpression,
                                                                 @QueryParam("sort") SortOrder sortOrder,
                                                                 @QueryParam("sortField") String sortField) {
        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder).build();
        monitor.debug(format("get all contract definitions %s", spec));

        return service.query(spec).stream()
                .map(it -> transformerRegistry.transform(it, ContractDefinitionDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Override
    public ContractDefinitionDto getContractDefinition(@PathParam("id") String id) {
        monitor.debug(format("get contract definition with ID %s", id));

        return Optional.of(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, ContractDefinitionDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Override
    public void createContractDefinition(@Valid ContractDefinitionDto dto) {
        monitor.debug("create new contract definition");
        var transformResult = transformerRegistry.transform(dto, ContractDefinition.class);
        if (transformResult.failed()) {
            throw new IllegalArgumentException("Request is not well formatted");
        }

        ContractDefinition contractDefinition = transformResult.getContent();

        var result = service.create(contractDefinition);
        if (result.succeeded()) {
            monitor.debug(format("Contract negotiation created %s", result.getContent().getId()));
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
            monitor.debug(format("Contract negotiation deleted %s", result.getContent().getId()));
        } else {
            handleFailedResult(result, id);
        }
    }

    private void handleFailedResult(ServiceResult<ContractDefinition> result, String id) {
        switch (result.reason()) {
            case NOT_FOUND:
                throw new ObjectNotFoundException(ContractDefinition.class, id);
            case CONFLICT:
                throw new ObjectExistsException(ContractDefinition.class, id);
            default:
                throw new EdcException("unexpected error");
        }
    }

}
