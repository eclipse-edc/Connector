/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.transferprocess;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TerminateTransferDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferState;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/transferprocesses")
public class TransferProcessNewApiController implements TransferProcessNewApi {

    private final Monitor monitor;
    private final TransferProcessService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;

    public TransferProcessNewApiController(Monitor monitor, TransferProcessService service, TypeTransformerRegistry transformerRegistry, JsonLd jsonLd) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.jsonLd = jsonLd;
    }

    @POST
    @Path("request")
    @Override
    public List<JsonObject> queryTransferProcesses(JsonObject querySpecDto) {
        var querySpec = jsonLd.expand(querySpecDto)
                .compose(expanded -> transformerRegistry.transform(expanded, QuerySpecDto.class))
                .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class))
                .orElseThrow(InvalidRequestException::new);

        try (var stream = service.query(querySpec).orElseThrow(exceptionMapper(PolicyDefinition.class))) {
            return stream
                    .map(policyDefinition -> transformerRegistry.transform(policyDefinition, TransferProcessDto.class)
                            .compose(dto -> transformerRegistry.transform(dto, JsonObject.class)
                            .compose(jsonLd::compact))
                            .onFailure(f -> monitor.warning(f.getFailureDetail())))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toList());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getTransferProcess(@PathParam("id") String id) {
        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        return transformerRegistry.transform(definition, TransferProcessDto.class)
                .compose(dto -> transformerRegistry.transform(dto, JsonObject.class))
                .compose(jsonLd::compact)
                .onFailure(f -> monitor.warning(f.getFailureDetail()))
                .orElseThrow(failure -> new ObjectNotFoundException(PolicyDefinition.class, id));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferProcessState(@PathParam("id") String id) {
        return Optional.of(id)
                .map(service::getState)
                .map(TransferState::new)
                .map(state -> transformerRegistry.transform(state, JsonObject.class)
                        .compose(jsonLd::compact)
                        .onFailure(f -> monitor.warning(f.getFailureDetail()))
                        .orElseThrow(failure -> new ObjectNotFoundException(TransferProcess.class, id)))
                .orElseThrow(() -> new ObjectNotFoundException(TransferProcess.class, id));
    }

    @POST
    @Override
    public JsonObject initiateTransferProcess(JsonObject request) {
        var transferRequest = jsonLd.expand(request)
                .compose(json -> transformerRegistry.transform(json, TransferRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, TransferRequest.class))
                .orElseThrow(InvalidRequestException::new);

        var createdTransfer = service.initiateTransfer(transferRequest)
                .onSuccess(d -> monitor.debug(format("Transfer Process created %s", d.getId())))
                .orElseThrow(it -> mapToException(it, TransferProcess.class));

        var responseDto = IdResponseDto.Builder.newInstance()
                .id(createdTransfer.getId())
                .createdAt(createdTransfer.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("/{id}/deprovision")
    @Override
    public void deprovisionTransferProcess(@PathParam("id") String id) {
        service.deprovision(id)
                .onSuccess(tp -> monitor.debug(format("Deprovision requested for TransferProcess with ID %s", tp.getId())))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateTransferProcess(@PathParam("id") String id, JsonObject requestBody) {
        var dto = jsonLd.expand(requestBody)
                .compose(json -> transformerRegistry.transform(requestBody, TerminateTransferDto.class))
                .orElseThrow(InvalidRequestException::new);

        service.terminate(id, dto.getReason())
                .onSuccess(tp -> monitor.debug(format("Termination requested for TransferProcess with ID %s", tp.getId())))
                .orElseThrow(failure -> mapToException(failure, TransferProcess.class, id));
    }
}
