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

package org.eclipse.edc.protocol.dsp.api.configuration.message;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.protocol.dsp.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.spi.message.MessageSpecHandler;
import org.eclipse.edc.protocol.dsp.spi.message.PostDspRequest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.api.configuration.error.DspErrorResponse.type;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

public class MessageSpecHandlerImpl implements MessageSpecHandler {

    private final Monitor monitor;
    private final String callbackAddress;
    private final IdentityService identityService;
    private final JsonObjectValidatorRegistry validatorRegistry;
    private final TypeTransformerRegistry transformerRegistry;

    public MessageSpecHandlerImpl(Monitor monitor, String callbackAddress, IdentityService identityService,
                                  JsonObjectValidatorRegistry validatorRegistry, TypeTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.callbackAddress = callbackAddress;
        this.identityService = identityService;
        this.validatorRegistry = validatorRegistry;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public <R> Response getResource(GetDspRequest<R> dspRequest) {
        monitor.debug(() -> "DSP: Incoming resource request for %s id %s".formatted(dspRequest.getResultClass(), dspRequest.getId()));

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(dspRequest.getToken()).build();
        var claimTokenResult = identityService.verifyJwtToken(tokenRepresentation, callbackAddress);

        if (claimTokenResult.failed()) {
            monitor.debug(() -> "DSP: Unauthorized: %s".formatted(claimTokenResult.getFailureDetail()));
            return type(dspRequest.getErrorType()).unauthorized();
        }

        var serviceResult = dspRequest.getServiceCall().apply(dspRequest.getId(), claimTokenResult.getContent());
        if (serviceResult.failed()) {
            return type(dspRequest.getErrorType()).processId(dspRequest.getId()).from(serviceResult.getFailure());
        }

        var resource = serviceResult.getContent();

        var transformation = transformerRegistry.transform(resource, JsonObject.class);
        if (transformation.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(String.format("Error transforming %s, error id %s: %s", dspRequest.getResultClass().getSimpleName(), errorCode, transformation.getFailureDetail()));
            return type(dspRequest.getErrorType()).processId(dspRequest.getId()).message(String.format("Error code %s", errorCode)).internalServerError();
        }

        return Response.ok().type(MediaType.APPLICATION_JSON).entity(transformation.getContent()).build();
    }

    @Override
    public <I extends RemoteMessage, R> Response createResource(PostDspRequest<I, R> dspRequest) {
        monitor.debug(() -> "DSP: Incoming %s for %s process%s".formatted(
                dspRequest.getInputClass().getSimpleName(),
                dspRequest.getResultClass(),
                dspRequest.getProcessId() != null ? ": " + dspRequest.getProcessId() : ""));

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(dspRequest.getToken()).build();
        var claimTokenResult = identityService.verifyJwtToken(tokenRepresentation, callbackAddress);

        if (claimTokenResult.failed()) {
            monitor.debug(() -> "DSP: Unauthorized: %s".formatted(claimTokenResult.getFailureDetail()));
            return type(dspRequest.getErrorType()).unauthorized();
        }

        var validation = validatorRegistry.validate(dspRequest.getExpectedMessageType(), dspRequest.getMessage());
        if (validation.failed()) {
            monitor.debug(format("DSP: Validation failed: %s", validation.getFailureMessages()));
            return type(dspRequest.getErrorType()).badRequest();
        }

        var inputTransformation = transformerRegistry.transform(dspRequest.getMessage(), dspRequest.getInputClass())
                .compose(message -> {
                    if (message instanceof ProcessRemoteMessage processRemoteMessage) {
                        processRemoteMessage.setProtocol(DATASPACE_PROTOCOL_HTTP);

                        return Objects.equals(dspRequest.getProcessId(), processRemoteMessage.getProcessId())
                                ? Result.success(message)
                                : Result.failure("DSP: Invalid process ID. Expected: %s, actual: %s"
                                    .formatted(dspRequest.getProcessId(), processRemoteMessage.getProcessId()));
                    } else {
                        return Result.success(message);
                    }

                });

        if (inputTransformation.failed()) {
            monitor.debug(format("DSP: Transformation failed: %s", validation.getFailureMessages()));
            return type(dspRequest.getErrorType()).badRequest();
        }

        var serviceResult = dspRequest.getServiceCall().apply(inputTransformation.getContent(), claimTokenResult.getContent());
        if (serviceResult.failed()) {
            return type(dspRequest.getErrorType()).from(serviceResult.getFailure());
        }

        var resource = serviceResult.getContent();

        var outputTransformation = transformerRegistry.transform(resource, JsonObject.class);
        if (outputTransformation.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning(String.format("Error transforming %s, error id %s: %s", dspRequest.getResultClass().getSimpleName(), errorCode, outputTransformation.getFailureDetail()));
            return type(dspRequest.getErrorType()).message(String.format("Error code %s", errorCode)).internalServerError();
        }

        return Response.ok().type(MediaType.APPLICATION_JSON).entity(outputTransformation.getContent()).build();
    }

    @Override
    public <I extends RemoteMessage, R> Response updateResource(PostDspRequest<I, R> dspRequest) {
        monitor.debug(() -> "DSP: Incoming %s for %s process%s".formatted(
                dspRequest.getInputClass().getSimpleName(),
                dspRequest.getResultClass(),
                dspRequest.getProcessId() != null ? ": " + dspRequest.getProcessId() : ""));

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(dspRequest.getToken()).build();
        var claimTokenResult = identityService.verifyJwtToken(tokenRepresentation, callbackAddress);

        if (claimTokenResult.failed()) {
            monitor.debug(() -> "DSP: Unauthorized: %s".formatted(claimTokenResult.getFailureDetail()));
            return type(dspRequest.getErrorType()).processId(dspRequest.getProcessId()).unauthorized();
        }

        var validation = validatorRegistry.validate(dspRequest.getExpectedMessageType(), dspRequest.getMessage());
        if (validation.failed()) {
            monitor.debug(format("DSP: Validation failed: %s", validation.getFailureMessages()));
            return type(dspRequest.getErrorType()).processId(dspRequest.getProcessId()).badRequest();
        }

        var inputTransformation = transformerRegistry.transform(dspRequest.getMessage(), dspRequest.getInputClass())
                .compose(message -> {
                    if (message instanceof ProcessRemoteMessage processRemoteMessage) {
                        processRemoteMessage.setProtocol(DATASPACE_PROTOCOL_HTTP);

                        return Objects.equals(dspRequest.getProcessId(), processRemoteMessage.getProcessId())
                                ? Result.success(message)
                                : Result.failure("DSP: Invalid process ID. Expected: %s, actual: %s"
                                    .formatted(dspRequest.getProcessId(), processRemoteMessage.getProcessId()));
                    } else {
                        return Result.success(message);
                    }

                });

        if (inputTransformation.failed()) {
            monitor.debug(format("DSP: Transformation failed: %s", validation.getFailureMessages()));
            return type(dspRequest.getErrorType()).processId(dspRequest.getProcessId()).badRequest();
        }

        var serviceResult = dspRequest.getServiceCall().apply(inputTransformation.getContent(), claimTokenResult.getContent());
        if (serviceResult.failed()) {
            return type(dspRequest.getErrorType()).processId(dspRequest.getProcessId()).from(serviceResult.getFailure());
        }

        return Response.ok().type(MediaType.APPLICATION_JSON).build();
    }

    public <I extends RemoteMessage, R> ServiceResult<R> handlePostRequest(PostDspRequest<I, R> dspMessage) {
        monitor.debug(() -> "DSP: Incoming %s for %s process%s".formatted(
                dspMessage.getInputClass().getSimpleName(),
                dspMessage.getResultClass(),
                dspMessage.getProcessId() != null ? ": " + dspMessage.getProcessId() : ""));

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(dspMessage.getToken()).build();
        var claimTokenResult = identityService.verifyJwtToken(tokenRepresentation, callbackAddress);

        if (claimTokenResult.failed()) {
            monitor.debug(() -> "DSP: Unauthorized: %s".formatted(claimTokenResult.getFailureDetail()));
            return ServiceResult.unauthorized(claimTokenResult.getFailureMessages());
        }

        var validation = validatorRegistry.validate(dspMessage.getExpectedMessageType(), dspMessage.getMessage());
        if (validation.failed()) {
            monitor.debug(format("DSP: Validation failed: %s", validation.getFailureMessages()));
            return ServiceResult.badRequest(validation.getFailureMessages());
        }

        var transformation = transformerRegistry.transform(dspMessage.getMessage(), dspMessage.getInputClass())
                .compose(message -> {
                    if (message instanceof ProcessRemoteMessage processRemoteMessage) {
                        processRemoteMessage.setProtocol(DATASPACE_PROTOCOL_HTTP);

                        return Objects.equals(dspMessage.getProcessId(), processRemoteMessage.getProcessId())
                                ? Result.success(message)
                                : Result.failure("DSP: Invalid process ID. Expected: %s, actual: %s"
                                .formatted(dspMessage.getProcessId(), processRemoteMessage.getProcessId()));
                    } else {
                        return Result.success(message);
                    }

                });

        if (transformation.failed()) {
            monitor.debug(format("DSP: Transformation failed: %s", validation.getFailureMessages()));
            return ServiceResult.badRequest(validation.getFailureMessages());
        }

        return dspMessage.getServiceCall().apply(transformation.getContent(), claimTokenResult.getContent());
    }
}
