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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.api.management.contractagreement;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.contractagreement.model.ContractAgreementDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
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
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/contractagreements")
public class ContractAgreementApiController implements ContractAgreementApi {
    private final Monitor monitor;
    private final ContractAgreementService service;
    private final DtoTransformerRegistry transformerRegistry;

    public ContractAgreementApiController(Monitor monitor, ContractAgreementService service, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Override
    @Deprecated
    public List<ContractAgreementDto> getAllAgreements(@Valid @BeanParam QuerySpecDto querySpecDto) {
        return queryContractAgreements(querySpecDto);
    }

    @POST
    @Path("/request")
    @Override
    public List<ContractAgreementDto> queryAllAgreements(@Valid QuerySpecDto querySpecDto) {
        return queryContractAgreements(ofNullable(querySpecDto).orElse(QuerySpecDto.Builder.newInstance().build()));
    }

    @GET
    @Path("{id}")
    @Override
    public ContractAgreementDto getContractAgreement(@PathParam("id") String id) {
        monitor.debug(format("get contract agreement with ID %s", id));

        return Optional.of(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractAgreement.class, id));
    }

    @NotNull
    private List<ContractAgreementDto> queryContractAgreements(QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        monitor.debug(format("get all contract agreements from %s", spec));

        var queryResult = service.query(spec);

        //will throw an exception
        if (queryResult.failed()) {
            throw mapToException(queryResult, ContractDefinition.class, null);
        }

        try (var stream = queryResult.getContent()) {
            return stream
                    .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(Collectors.toList());
        }
    }


}
