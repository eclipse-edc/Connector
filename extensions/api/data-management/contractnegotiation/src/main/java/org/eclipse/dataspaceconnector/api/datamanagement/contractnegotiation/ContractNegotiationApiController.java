/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 *    Microsoft Corporation - Added initiate-negotiation endpoint
 *    Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
    public List<ContractNegotiationDto> getNegotiations(@QueryParam("offset") Integer offset,
                                                        @QueryParam("limit") Integer limit,
                                                        @QueryParam("filter") String filterExpression,
                                                        @QueryParam("sort") SortOrder sortOrder,
                                                        @QueryParam("sortField") String sortField) {
        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder)
                .build();

        monitor.debug(format("Get all contract definitions %s", spec));

        return service.query(spec).stream()
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
    public String getNegotiationState(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation state with id %s", id));
        return Optional.of(id)
                .map(service::getState)
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
    public String initiateContractNegotiation(NegotiationInitiateRequestDto initiateDto) {
        if (!isValid(initiateDto)) {
            throw new IllegalArgumentException("Negotiation request is invalid");
        }

        var transformResult = transformerRegistry.transform(initiateDto, ContractOfferRequest.class);
        if (transformResult.failed()) {
            throw new IllegalArgumentException("Negotiation request is invalid");
        }

        var request = transformResult.getContent();

        ContractNegotiation contractNegotiation = service.initiateNegotiation(request);
        return contractNegotiation.getId();
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
            handleFailedResult(result, id);
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
            handleFailedResult(result, id);
        }
    }

    private boolean isValid(NegotiationInitiateRequestDto initiateDto) {
        return StringUtils.isNoneBlank(initiateDto.getConnectorId(), initiateDto.getConnectorAddress(), initiateDto.getProtocol(), initiateDto.getOfferId());
    }

    private void handleFailedResult(ServiceResult<ContractNegotiation> result, String id) {
        switch (result.reason()) {
            case NOT_FOUND: throw new ObjectNotFoundException(ContractNegotiation.class, id);
            case CONFLICT: throw new ObjectExistsException(ContractNegotiation.class, id);
            default: throw new EdcException("unexpected error");
        }
    }
}
