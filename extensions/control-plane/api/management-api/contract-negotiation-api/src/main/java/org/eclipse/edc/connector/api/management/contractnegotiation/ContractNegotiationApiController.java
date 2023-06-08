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

package org.eclipse.edc.connector.api.management.contractnegotiation;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationState;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })

@Path("/v2/contractnegotiations")
public class ContractNegotiationApiController implements ContractNegotiationApi {
    private final ContractNegotiationService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public ContractNegotiationApiController(ContractNegotiationService service, TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryNegotiations(JsonObject querySpecDto) {
        var spec = ofNullable(querySpecDto)
                .map(json -> transformerRegistry.transform(json, QuerySpecDto.class)
                        .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class)))
                .orElse(Result.success(QuerySpec.Builder.newInstance().build()))
                .orElseThrow(InvalidRequestException::new);

        try (var stream = service.query(spec).orElseThrow(exceptionMapper(ContractNegotiation.class, null))) {
            return stream
                    .map(it -> transformerRegistry.transform(it, ContractNegotiationDto.class)
                            .compose(dto -> transformerRegistry.transform(dto, JsonObject.class)))
                    .peek(this::logIfError)
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toJsonArray());
        }
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getNegotiation(@PathParam("id") String id) {

        return Optional.of(id)
                .map(service::findbyId)
                .map(it -> transformerRegistry.transform(it, ContractNegotiationDto.class)
                        .compose(dto -> transformerRegistry.transform(dto, JsonObject.class))
                        .orElseThrow(InvalidRequestException::new))
                .orElseThrow(() -> new ObjectNotFoundException(ContractNegotiation.class, id));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getNegotiationState(@PathParam("id") String id) {
        return Optional.of(id)
                .map(service::getState)
                .map(NegotiationState::new)
                .map(state -> transformerRegistry.transform(state, JsonObject.class))
                .orElseThrow(() -> new ObjectNotFoundException(ContractNegotiation.class, id))
                .orElseThrow(failure -> new EdcException(failure.getFailureDetail()));
    }

    @GET
    @Path("/{id}/agreement")
    @Override
    public JsonObject getAgreementForNegotiation(@PathParam("id") String negotiationId) {
        return Optional.of(negotiationId)
                .map(service::getForNegotiation)
                .map(it -> transformerRegistry.transform(it, ContractAgreementDto.class)
                        .compose(dto -> transformerRegistry.transform(dto, JsonObject.class))
                        .orElseThrow(failure -> new EdcException(failure.getFailureDetail())))
                .orElseThrow(() -> new ObjectNotFoundException(ContractNegotiation.class, negotiationId));
    }

    @POST
    @Override
    public JsonObject initiateContractNegotiation(JsonObject requestObject) {
        var contractRequest = transformerRegistry.transform(requestObject, NegotiationInitiateRequestDto.class)
                .compose(dto -> transformerRegistry.transform(dto, ContractRequest.class))
                .orElseThrow(InvalidRequestException::new);


        var contractNegotiation = service.initiateNegotiation(contractRequest);

        var responseDto = IdResponseDto.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("/{id}/cancel")
    @Override
    public void cancelNegotiation(@PathParam("id") String id) {
        service.cancel(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }

    @POST
    @Path("/{id}/decline")
    @Override
    public void declineNegotiation(@PathParam("id") String id) {
        service.decline(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }


    private void logIfError(Result<?> result) {
        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }
}
