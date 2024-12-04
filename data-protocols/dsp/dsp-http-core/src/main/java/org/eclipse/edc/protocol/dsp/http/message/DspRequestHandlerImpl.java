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

package org.eclipse.edc.protocol.dsp.http.message;

import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.ResponseDecorator;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.types.domain.message.ErrorMessage;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DspRequestHandlerImpl implements DspRequestHandler {

    public static final String UNAUTHORIZED = "Unauthorized.";
    public static final String BAD_REQUEST = "Bad request.";
    public static final String INTERNAL_ERROR = "Error code %s";
    private final Monitor monitor;
    private final JsonObjectValidatorRegistry validatorRegistry;
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry;

    public DspRequestHandlerImpl(Monitor monitor, JsonObjectValidatorRegistry validatorRegistry, DspProtocolTypeTransformerRegistry dspTransformerRegistry) {
        this.monitor = monitor;
        this.validatorRegistry = validatorRegistry;
        this.dspTransformerRegistry = dspTransformerRegistry;
    }

    @Override
    public <R, E extends ErrorMessage> Response getResource(GetDspRequest<R, E> request) {
        monitor.debug(() -> "DSP: Incoming resource request for %s id %s".formatted(request.getResultClass(), request.getId()));

        var token = request.getToken();
        if (token == null) {
            return unauthorized(request);
        }
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();

        var serviceResult = request.getServiceCall().apply(request.getId(), tokenRepresentation);
        if (serviceResult.failed()) {
            monitor.debug(() -> "DSP: Service call failed: %s".formatted(serviceResult.getFailureDetail()));
            return forFailure(serviceResult.getFailure(), request);
        }

        var resource = serviceResult.getContent();

        var registryResult = dspTransformerRegistry.forProtocol(request.getProtocol());
        if (registryResult.failed()) {
            monitor.debug(() -> "DSP: Unsupported protocol %s: %s".formatted(request.getProtocol(), registryResult.getFailureMessages()));
            return badRequest(request);
        }
        var registry = registryResult.getContent();
        var transformation = registry.transform(resource, JsonObject.class);
        if (transformation.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning("Error transforming %s, error id %s: %s".formatted(request.getResultClass().getSimpleName(), errorCode, transformation.getFailureDetail()));
            return internalServerError(request, errorCode.toString());
        }

        return Response.ok().type(MediaType.APPLICATION_JSON).entity(transformation.getContent()).build();
    }

    @Override
    public <I extends RemoteMessage, R, E extends ErrorMessage> Response createResource(PostDspRequest<I, R, E> request, ResponseDecorator<I, R> responseDecorator) {
        monitor.debug(() -> "DSP: Incoming %s for %s process%s".formatted(
                request.getInputClass().getSimpleName(),
                request.getResultClass(),
                request.getProcessId() != null ? ": " + request.getProcessId() : ""));

        var token = request.getToken();
        if (token == null) {
            monitor.severe("DSP: No auth token provided - returning 401");
            return unauthorized(request);
        }

        var validation = validatorRegistry.validate(request.getExpectedMessageType(), request.getMessage());
        if (validation.failed()) {
            monitor.debug(() -> "DSP: Validation failed: %s".formatted(validation.getFailureMessages()));
            return badRequest(request);
        }

        var registryResult = dspTransformerRegistry.forProtocol(request.getProtocol());

        if (registryResult.failed()) {
            monitor.debug(() -> "DSP: Unsupported protocol %s: %s".formatted(request.getProtocol(), registryResult.getFailureMessages()));
            return badRequest(request);
        }

        var registry = registryResult.getContent();
        var inputTransformation = registry.transform(request.getMessage(), request.getInputClass())
                .compose(message -> {
                    if (message instanceof ProtocolRemoteMessage protocolRemoteMessage) {
                        protocolRemoteMessage.setProtocol(request.getProtocol());
                    }
                    return Result.success(message);
                });

        if (inputTransformation.failed()) {
            monitor.debug(() -> "DSP: Transformation failed: %s".formatted(inputTransformation.getFailureMessages()));
            return badRequest(request);
        }

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();

        var input = inputTransformation.getContent();
        var serviceResult = request.getServiceCall().apply(input, tokenRepresentation);
        if (serviceResult.failed()) {
            monitor.debug(() -> "DSP: Service call failed: %s".formatted(serviceResult.getFailureDetail()));
            return forFailure(serviceResult.getFailure(), request);
        }

        var resource = serviceResult.getContent();

        var outputTransformation = registry.transform(resource, JsonObject.class);
        if (outputTransformation.failed()) {
            var errorCode = UUID.randomUUID();
            monitor.warning("Error transforming %s, error id %s: %s".formatted(request.getResultClass().getSimpleName(), errorCode, outputTransformation.getFailureDetail()));
            return internalServerError(request, errorCode.toString());
        }

        return responseDecorator.decorate(Response.ok(), input, resource)
                .type(MediaType.APPLICATION_JSON)
                .entity(outputTransformation.getContent())
                .build();
    }

    @Override
    public <I extends RemoteMessage, R, E extends ErrorMessage> Response updateResource(PostDspRequest<I, R, E> request) {
        monitor.debug(() -> "DSP: Incoming %s for %s process%s".formatted(
                request.getInputClass().getSimpleName(),
                request.getResultClass(),
                request.getProcessId() != null ? ": " + request.getProcessId() : ""));

        var token = request.getToken();
        if (token == null) {
            return unauthorized(request);
        }

        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(request.getToken()).build();

        var validation = validatorRegistry.validate(request.getExpectedMessageType(), request.getMessage());
        if (validation.failed()) {
            monitor.debug(() -> "DSP: Validation failed: %s".formatted(validation.getFailureMessages()));
            return badRequest(request);
        }

        var registryResult = dspTransformerRegistry.forProtocol(request.getProtocol());

        if (registryResult.failed()) {
            monitor.debug(() -> "DSP: Unsupported protocol %s: %s".formatted(request.getProtocol(), registryResult.getFailureMessages()));
            return badRequest(request);
        }

        var registry = registryResult.getContent();

        var inputTransformation = registry.transform(request.getMessage(), request.getInputClass())
                .compose(message -> {
                    if (message instanceof ProcessRemoteMessage processRemoteMessage) {
                        var processIdValidation = processRemoteMessage.isValidProcessId(request.getProcessId());
                        if (processIdValidation.succeeded()) {
                            processRemoteMessage.setProcessId(request.getProcessId());
                            processRemoteMessage.setProtocol(request.getProtocol());
                            return Result.success(message);
                        } else {
                            return Result.failure("DSP: %s".formatted(processIdValidation.getFailureDetail()));
                        }
                    } else {
                        return Result.success(message);
                    }
                });

        if (inputTransformation.failed()) {
            monitor.debug(() -> "DSP: Transformation failed: %s".formatted(inputTransformation.getFailureMessages()));
            return badRequest(request);
        }

        return request.getServiceCall()
                .apply(inputTransformation.getContent(), tokenRepresentation)
                .map(it -> Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build())
                .orElse(failure -> {
                    monitor.debug(() -> "DSP: Service call failed: %s".formatted(failure.getFailureDetail()));
                    return forFailure(failure, request);
                });
    }

    private <I extends RemoteMessage, R, E extends ErrorMessage> Response forFailure(ServiceFailure failure, PostDspRequest<I, R, E> request) {
        return forFailure(failure, request.getProtocol(), request.getErrorProvider().get().processId(request.getProcessId()));
    }

    private <R, E extends ErrorMessage> Response forFailure(ServiceFailure failure, GetDspRequest<R, E> request) {
        return forFailure(failure, request.getProtocol(), request.getErrorProvider().get().processId(request.getId()));
    }

    private <E extends ErrorMessage> Response forFailure(ServiceFailure failure, String protocol, ErrorMessage.Builder<E, ?> builder) {
        var code = getHttpStatus(failure);
        return forStatus(code, protocol, failure.getMessages(), builder);
    }

    private <R, E extends ErrorMessage> Response unauthorized(GetDspRequest<R, E> request) {
        return forStatus(Response.Status.UNAUTHORIZED, List.of(UNAUTHORIZED), request);
    }

    private <I extends RemoteMessage, R, E extends ErrorMessage> Response unauthorized(PostDspRequest<I, R, E> request) {
        return forStatus(Response.Status.UNAUTHORIZED, List.of(UNAUTHORIZED), request);
    }

    private <R, E extends ErrorMessage> Response badRequest(GetDspRequest<R, E> request) {
        return forStatus(Response.Status.BAD_REQUEST, List.of(BAD_REQUEST), request);
    }

    private <I extends RemoteMessage, R, E extends ErrorMessage> Response badRequest(PostDspRequest<I, R, E> request) {
        return forStatus(Response.Status.BAD_REQUEST, List.of(BAD_REQUEST), request);
    }

    private <R, E extends ErrorMessage> Response internalServerError(GetDspRequest<R, E> request, String errorCode) {
        return forStatus(Response.Status.INTERNAL_SERVER_ERROR, List.of(INTERNAL_ERROR.formatted(errorCode)), request);
    }

    private <I extends RemoteMessage, R, E extends ErrorMessage> Response internalServerError(PostDspRequest<I, R, E> request, String errorCode) {
        return forStatus(Response.Status.INTERNAL_SERVER_ERROR, List.of(INTERNAL_ERROR.formatted(errorCode)), request);
    }

    private <I extends RemoteMessage, R, E extends ErrorMessage> Response forStatus(Response.Status status, List<String> messages, PostDspRequest<I, R, E> request) {
        return forStatus(status, request.getProtocol(), messages, request.getErrorProvider().get().processId(request.getProcessId()));
    }

    private <R, E extends ErrorMessage> Response forStatus(Response.Status status, List<String> messages, GetDspRequest<R, E> request) {
        return forStatus(status, request.getProtocol(), messages, request.getErrorProvider().get().processId(request.getId()));
    }

    private <E extends ErrorMessage> Response forStatus(Response.Status statusCode, String protocol,
                                                        List<String> messages, ErrorMessage.Builder<E, ?> builder) {

        builder.code(Integer.toString(statusCode.getStatusCode()));
        builder.messages(messages);

        var body = dspTransformerRegistry.forProtocol(protocol)
                .compose(registry -> registry.transform(builder.build(), JsonObject.class))
                .onFailure(f -> monitor.debug(() -> "DSP: Transformation failed: %s".formatted(f.getMessages())))
                .orElseThrow(f -> new EdcException("DSP: Transformation failed: %s".formatted(f.getMessages())));

        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    @NotNull
    private Response.Status getHttpStatus(ServiceFailure failure) {
        return switch (failure.getReason()) {
            case UNAUTHORIZED -> Response.Status.UNAUTHORIZED;
            case CONFLICT -> Response.Status.CONFLICT;
            case NOT_FOUND -> Response.Status.NOT_FOUND;
            default -> Response.Status.BAD_REQUEST;
        };
    }
}
