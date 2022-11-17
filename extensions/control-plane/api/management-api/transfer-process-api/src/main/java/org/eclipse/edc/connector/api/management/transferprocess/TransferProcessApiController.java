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

package org.eclipse.edc.connector.api.management.transferprocess;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferState;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/transferprocess")
public class TransferProcessApiController implements TransferProcessApi {
    private final Monitor monitor;
    private final TransferProcessService service;
    private final DtoTransformerRegistry transformerRegistry;

    public TransferProcessApiController(Monitor monitor, TransferProcessService service, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Path("/request")
    @Override
    public List<TransferProcessDto> queryAllTransferProcesses(@Valid QuerySpecDto querySpecDto) {
        return queryTransferProcesses(ofNullable(querySpecDto).orElse(QuerySpecDto.Builder.newInstance().build()));
    }

    @GET
    @Deprecated
    @Override
    public List<TransferProcessDto> getAllTransferProcesses(@Valid @BeanParam QuerySpecDto querySpecDto) {
        return queryTransferProcesses(querySpecDto);
    }

    @GET
    @Path("/{id}")
    @Override
    public TransferProcessDto getTransferProcess(@PathParam("id") String id) {
        return Optional.of(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, TransferProcessDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(TransferProcess.class, id));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public TransferState getTransferProcessState(@PathParam("id") String id) {
        return Optional.of(id)
                .map(service::getState)
                .map(TransferState::new)
                .orElseThrow(() -> new ObjectNotFoundException(TransferProcess.class, id));
    }

    @POST
    @Path("/{id}/cancel")
    @Override
    public void cancelTransferProcess(@PathParam("id") String id) {
        monitor.debug("Cancelling TransferProcess with ID " + id);
        var transferProcess = service.cancel(id).orElseThrow(exceptionMapper(TransferProcess.class, id));
        monitor.debug(format("Transfer process canceled %s", transferProcess.getId()));
    }

    @POST
    @Path("/{id}/deprovision")
    @Override
    public void deprovisionTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("Attempting to deprovision TransferProcess with id %s", id));
        var transferProcess = service.deprovision(id).orElseThrow(exceptionMapper(TransferProcess.class, id));
        monitor.debug(format("Transfer process deprovisioned %s", transferProcess.getId()));
    }

    @POST
    @Override
    public IdResponseDto initiateTransfer(@Valid TransferRequestDto transferRequest) {
        var transformResult = transformerRegistry.transform(transferRequest, DataRequest.class);
        if (transformResult.failed()) {
            throw new InvalidRequestException(transformResult.getFailureMessages());
        }
        monitor.debug("Starting transfer for asset " + transferRequest.getAssetId());

        var dataRequest = transformResult.getContent();
        var result = service.initiateTransfer(dataRequest).orElseThrow(exceptionMapper(TransferProcess.class, transferRequest.getId()));
        return IdResponseDto.Builder.newInstance()
                .id(result)
                //To be accurate createdAt should come from the transfer object
                .createdAt(Clock.systemUTC().millis())
                .build();
    }

    private List<TransferProcessDto> queryTransferProcesses(QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            throw new InvalidRequestException(result.getFailureMessages());
        }

        var spec = result.getContent();

        try (var stream = service.query(spec).orElseThrow(exceptionMapper(TransferProcess.class, null))) {
            return stream
                    .map(tp -> transformerRegistry.transform(tp, TransferProcessDto.class))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(Collectors.toList());
        }
    }


}
