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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer.SUSPEND_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer.TERMINATE_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;


public abstract class BaseTransferProcessApiController {

    protected final Monitor monitor;
    protected final SingleParticipantContextSupplier participantContextSupplier;
    private final TransferProcessService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validatorRegistry;


    public BaseTransferProcessApiController(Monitor monitor, TransferProcessService service,
                                            TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry, SingleParticipantContextSupplier participantContextSupplier) {
        this.monitor = monitor;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validatorRegistry = validatorRegistry;
        this.participantContextSupplier = participantContextSupplier;
    }

    public JsonArray queryTransferProcesses(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.none();
        } else {
            validatorRegistry.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.search(querySpec).orElseThrow(exceptionMapper(TransferProcess.class)).stream()
                .map(transferProcess -> transformerRegistry.transform(transferProcess, JsonObject.class)
                        .onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }


    public JsonObject getTransferProcess(String id) {
        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(TransferProcess.class, id);
        }

        return transformerRegistry.transform(definition, JsonObject.class)
                .onFailure(f -> monitor.warning(f.getFailureDetail()))
                .orElseThrow(failure -> new ObjectNotFoundException(TransferProcess.class, id));
    }


    public JsonObject getTransferProcessState(String id) {
        return Optional.of(id)
                .map(service::getState)
                .map(TransferState::new)
                .map(state -> transformerRegistry.transform(state, JsonObject.class)
                        .onFailure(f -> monitor.warning(f.getFailureDetail()))
                        .orElseThrow(failure -> new ObjectNotFoundException(TransferProcess.class, id)))
                .orElseThrow(() -> new ObjectNotFoundException(TransferProcess.class, id));
    }


    public JsonObject initiateTransferProcess(JsonObject request) {
        validatorRegistry.validate(TRANSFER_REQUEST_TYPE, request).orElseThrow(ValidationFailureException::new);

        var participantContext = participantContextSupplier.get()
                .orElseThrow(exceptionMapper(TransferProcess.class));

        var transferRequest = transformerRegistry.transform(request, TransferRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var createdTransfer = service.initiateTransfer(participantContext, transferRequest)
                .onSuccess(d -> monitor.debug(format("Transfer Process created %s", d.getId())))
                .orElseThrow(it -> mapToException(it, TransferProcess.class));

        var responseDto = IdResponse.Builder.newInstance()
                .id(createdTransfer.getId())
                .createdAt(createdTransfer.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }


    public void deprovisionTransferProcess(String id) {
        service.deprovision(id)
                .onSuccess(tp -> monitor.debug(format("Deprovision requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }


    public void terminateTransferProcess(String id, JsonObject requestBody) {
        validatorRegistry.validate(TERMINATE_TRANSFER_TYPE, requestBody).orElseThrow(ValidationFailureException::new);

        var terminateTransfer = transformerRegistry.transform(requestBody, TerminateTransfer.class)
                .orElseThrow(InvalidRequestException::new);

        service.terminate(new TerminateTransferCommand(id, terminateTransfer.reason()))
                .onSuccess(tp -> monitor.debug(format("Termination requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }


    public void suspendTransferProcess(String id, JsonObject requestBody) {
        validatorRegistry.validate(SUSPEND_TRANSFER_TYPE, requestBody).orElseThrow(ValidationFailureException::new);

        var suspendTransfer = transformerRegistry.transform(requestBody, SuspendTransfer.class)
                .orElseThrow(InvalidRequestException::new);

        service.suspend(new SuspendTransferCommand(id, suspendTransfer.reason()))
                .onSuccess(tp -> monitor.debug(format("Suspension requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }


    public void resumeTransferProcess(String id) {
        service.resume(new ResumeTransferCommand(id))
                .onSuccess(tp -> monitor.debug(format("Resumption requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }
}
