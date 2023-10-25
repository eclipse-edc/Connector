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

package org.eclipse.edc.connector.api.sts.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.edc.connector.api.sts.model.StsTokenErrorResponse;
import org.eclipse.edc.service.spi.result.ServiceFailure;

import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.UNAUTHORIZED;

/**
 * Exception mapper that catches the `StsTokenException` exception, map it to a response code with a detailed response body
 * The {@link StsTokenExceptionMapper} is translated into a {@link StsTokenErrorResponse}
 */
public class StsTokenExceptionMapper implements ExceptionMapper<StsTokenException> {

    private static final Map<ServiceFailure.Reason, Response.Status> STATUS_MAP = Map.of(
            UNAUTHORIZED, Response.Status.UNAUTHORIZED,
            NOT_FOUND, Response.Status.UNAUTHORIZED,
            BAD_REQUEST, Response.Status.BAD_REQUEST,
            CONFLICT, Response.Status.BAD_REQUEST
    );

    private static final Map<ServiceFailure.Reason, String> ERRORS_MAP = Map.of(
            UNAUTHORIZED, "invalid_client",
            NOT_FOUND, "invalid_client",
            BAD_REQUEST, "invalid_request",
            CONFLICT, "invalid_request"
    );

    private static final Map<ServiceFailure.Reason, String> ERROR_DETAILS_MAP = Map.of(
            UNAUTHORIZED, "Invalid client or Invalid client credentials",
            NOT_FOUND, "Invalid client or Invalid client credentials");


    public StsTokenExceptionMapper() {
    }

    @Override
    public Response toResponse(StsTokenException exception) {
        var failure = exception.getServiceFailure();
        var status = STATUS_MAP.getOrDefault(failure.getReason(), INTERNAL_SERVER_ERROR);
        var errorDescription = ERROR_DETAILS_MAP.getOrDefault(failure.getReason(), exception.getMessage());
        var errorCode = ERRORS_MAP.getOrDefault(failure.getReason(), "invalid_request");
        var error = new StsTokenErrorResponse(errorCode, errorDescription);
        return Response.status(status)
                .entity(error)
                .build();
    }

}
