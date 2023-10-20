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
 * Exception mapper that catches all the `StsTokenException` exceptions, map them to a response code with a detailed response body
 */
public class StsTokenExceptionMapper implements ExceptionMapper<StsTokenException> {

    private final Map<ServiceFailure.Reason, Response.Status> statusMap;

    private final Map<ServiceFailure.Reason, String> errorsMap;


    public StsTokenExceptionMapper() {

        statusMap = Map.of(
                UNAUTHORIZED, Response.Status.UNAUTHORIZED,
                NOT_FOUND, Response.Status.UNAUTHORIZED,
                BAD_REQUEST, Response.Status.BAD_REQUEST,
                CONFLICT, Response.Status.BAD_REQUEST
        );

        errorsMap = Map.of(
                UNAUTHORIZED, "invalid_client",
                NOT_FOUND, "invalid_client",
                BAD_REQUEST, "invalid_request",
                CONFLICT, "invalid_request"
        );
    }

    @Override
    public Response toResponse(StsTokenException exception) {
        var failure = exception.getServiceFailure();
        var status = statusMap.getOrDefault(failure.getReason(), INTERNAL_SERVER_ERROR);
        var errorCode = errorsMap.getOrDefault(failure.getReason(), "invalid_request");
        StsTokenErrorResponse error = new StsTokenErrorResponse(errorCode, exception.getMessage(), "");
        return Response.status(status)
                .entity(error)
                .build();
    }

}
