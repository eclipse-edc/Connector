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

package org.eclipse.edc.protocol.dsp.spi.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.protocol.dsp.DspError;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for building DSP error {@link Response}
 */
public class DspErrorResponse {

    private final String type;
    private String processId;
    private final List<String> messages = new ArrayList<>();

    public static DspErrorResponse type(String type) {
        return new DspErrorResponse(type);
    }

    public DspErrorResponse(String type) {
        this.type = type;
    }

    public Response from(ServiceFailure failure) {
        var code = getHttpStatus(failure);

        return internalBuild(code, failure.getMessages());
    }

    public Response unauthorized() {
        return internalBuild(Response.Status.UNAUTHORIZED, List.of("Token validation failed."));
    }

    public Response badRequest() {
        return internalBuild(Response.Status.BAD_REQUEST, List.of("Bad request."));
    }

    public Response notImplemented() {
        return internalBuild(Response.Status.NOT_IMPLEMENTED, List.of("Currently not supported."));
    }

    public Response internalServerError() {
        return internalBuild(Response.Status.INTERNAL_SERVER_ERROR, Collections.emptyList());
    }

    public DspErrorResponse processId(String processId) {
        this.processId = processId;
        return this;
    }

    public DspErrorResponse message(String message) {
        this.messages.add(message);
        return this;
    }

    private Response internalBuild(Response.Status code, List<String> baseMessages) {
        var builder = DspError.Builder.newInstance()
                .type(type)
                .code(Integer.toString(code.getStatusCode()));

        if (messages.isEmpty()) {
            builder.messages(baseMessages);
        } else {
            builder.messages(messages);
        }

        if (processId != null) {
            builder.processId(processId);
        }

        return Response.status(code)
                .type(MediaType.APPLICATION_JSON)
                .entity(builder.build().toJson())
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
