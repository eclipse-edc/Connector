/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - Added initiate-negotiation endpoint
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationState;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.model.StringResponseDto;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.api.ServiceResultHandler.mapToException;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })

@Path("/contractnegotiations")
public class ContractNegotiationApiController implements ContractNegotiationApi {
    private final Monitor monitor;
    private final ContractNegotiationService service;
    private final DtoTransformerRegistry transformerRegistry;

    public ContractNegotiationApiController(Monitor monitor, ContractNegotiationService service, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Override
    public List<ContractNegotiationDto> getNegotiations(@Valid @BeanParam QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        monitor.debug(format("Get all contract definitions %s", spec));

        var queryResult = service.query(spec);
        if (queryResult.failed()) {
            throw mapToException(queryResult, ContractNegotiation.class, null);
        }
        return queryResult.getContent().stream()
                .map(it -> transformerRegistry.transform(it, ContractNegotiationDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Override
    public ContractNegotiationDto getNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation with id %s", id));

        return Optional.of(id)
                .map(service::findbyId)
                .map(it -> transformerRegistry.transform(it, ContractNegotiationDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public NegotiationState getNegotiationState(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation state with id %s", id));
        return Optional.of(id)
                .map(service::getState)
                .map(NegotiationState::new)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @GET
    @Path("/{id}/agreement")
    @Override
    public ContractAgreementDto getAgreementForNegotiation(@PathParam("id") String negotiationId) {
        monitor.debug(format("Get contract agreement of negotiation with id %s", negotiationId));

        return Optional.of(negotiationId)
                .map(service::getForNegotiation)
                .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, negotiationId));
    }

    @POST
    @Override
    public StringResponseDto initiateContractNegotiation(@Valid NegotiationInitiateRequestDto initiateDto) {
        var transformResult = transformerRegistry.transform(initiateDto, ContractOfferRequest.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException(transformResult.getFailureMessages());
        }

        var request = transformResult.getContent();

        var contractNegotiation = service.initiateNegotiation(request);
        return StringResponseDto.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();
    }

    @POST
    @Path("/{id}/cancel")
    @Override
    public void cancelNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Attempting to cancel contract definition with id %s", id));
        var result = service.cancel(id);
        if (result.succeeded()) {
            monitor.debug(format("Contract negotiation canceled %s", result.getContent().getId()));
        } else {
            throw mapToException(result, ContractNegotiation.class, id);
        }
    }

    @POST
    @Path("/{id}/decline")
    @Override
    public void declineNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Attempting to decline contract definition with id %s", id));
        var result = service.decline(id);
        if (result.succeeded()) {
            monitor.debug(format("Contract negotiation declined %s", result.getContent().getId()));
        } else {
            throw mapToException(result, ContractNegotiation.class, id);
        }
    }

}
