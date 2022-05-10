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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.service.ContractAgreementService;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
    public List<ContractAgreementDto> getAllAgreements(@Valid @BeanParam QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            monitor.warning("Error transforming QuerySpec: " + String.join(", ", result.getFailureMessages()));
            throw new IllegalArgumentException("Cannot transform QuerySpecDto object");
        }

        var spec = result.getContent();

        monitor.debug(format("get all contract agreements from %s", spec));

        return service.query(spec).stream()
                .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
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

}
