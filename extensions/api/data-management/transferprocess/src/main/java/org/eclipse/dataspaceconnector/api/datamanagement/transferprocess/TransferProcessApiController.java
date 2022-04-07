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

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
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

    @GET
    @Override
    public List<TransferProcessDto> getAllTransferProcesses(@QueryParam("offset") Integer offset,
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
        monitor.debug(format("Get all TransferProcesses %s", spec));

        return service.query(spec).stream()
                .map(tp -> transformerRegistry.transform(tp, TransferProcessDto.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Override
    public TransferProcessDto getTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("Get TransferProcess with ID %s", id));

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
    public String getTransferProcessState(@PathParam("id") String id) {
        monitor.debug(format("Get TransferProcess State with ID %s", id));

        return Optional.of(id)
                .map(service::getState)
                .orElseThrow(() -> new ObjectNotFoundException(TransferProcess.class, id));
    }

    @POST
    @Path("/request")
    @Override
    public String initiateTransfer(TransferRequestDto transferRequest) {
        if (!isValid(transferRequest)) {
            throw new IllegalArgumentException("Transfer request body not valid");
        }
        var dataRequest = Optional.ofNullable(transformerRegistry.transform(transferRequest, DataRequest.class))
                .filter(AbstractResult::succeeded).map(AbstractResult::getContent);
        if (dataRequest.isEmpty()) {
            throw  new IllegalArgumentException("Error during transforming TransferRequestDto into DataRequest");
        }
        monitor.debug("Starting transfer for asset " + transferRequest.getAssetId());

        ServiceResult<String> result = service.initiateTransfer(dataRequest.get());
        if (result.succeeded()) {
            monitor.debug(format("Transfer process initialised %s", result.getContent()));
            return result.getContent();
        } else {
            String message = format("Error during initiating the transfer with assetId: %s", transferRequest.getAssetId());
            monitor.severe(message);
            throw new EdcException(message);
        }
    }

    @POST
    @Path("/{id}/cancel")
    @Override
    public void cancelTransferProcess(@PathParam("id") String id) {
        monitor.debug("Cancelling TransferProcess with ID " + id);
        var result = service.cancel(id);
        if (result.succeeded()) {
            monitor.debug(format("Transfer process canceled %s", result.getContent().getId()));
        } else {
            handleFailedResult(result, id);
        }
    }

    @POST
    @Path("/{id}/deprovision")
    @Override
    public void deprovisionTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("Attempting to deprovision TransferProcess with id %s", id));
        var result = service.deprovision(id);
        if (result.succeeded()) {
            monitor.debug(format("Transfer process deprovisioned %s", result.getContent().getId()));
        } else {
            handleFailedResult(result, id);
        }
    }

    private boolean isValid(TransferRequestDto transferRequest) {
        return !StringUtils.isNullOrBlank(transferRequest.getAssetId()) &&
                !StringUtils.isNullOrBlank(transferRequest.getConnectorAddress()) &&
                !StringUtils.isNullOrBlank(transferRequest.getContractId()) &&
                !StringUtils.isNullOrBlank(transferRequest.getProtocol()) &&
                transferRequest.getDataDestination() != null;
    }

    private void handleFailedResult(ServiceResult<TransferProcess> result, String id) {
        switch (result.reason()) {
            case NOT_FOUND: throw new ObjectNotFoundException(TransferProcess.class, id);
            case CONFLICT: throw new ObjectExistsException(TransferProcess.class, id);
            default: throw new EdcException("unexpected error");
        }
    }
}
