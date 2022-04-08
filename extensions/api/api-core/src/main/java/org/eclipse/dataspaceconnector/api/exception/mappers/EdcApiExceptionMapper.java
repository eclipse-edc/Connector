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
 */

package org.eclipse.dataspaceconnector.api.exception.mappers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.dataspaceconnector.api.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.api.exception.NotAuthorizedException;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotModifiableException;

import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

public class EdcApiExceptionMapper implements ExceptionMapper<Throwable> {
    private final Map<Class<? extends Throwable>, Response.Status> exceptionMap;

    public EdcApiExceptionMapper() {
        exceptionMap = Map.of(
                IllegalArgumentException.class, BAD_REQUEST,
                NullPointerException.class, BAD_REQUEST,
                AuthenticationFailedException.class, UNAUTHORIZED,
                NotAuthorizedException.class, FORBIDDEN,
                ObjectNotFoundException.class, NOT_FOUND,
                ObjectExistsException.class, CONFLICT,
                ObjectNotModifiableException.class, CONFLICT,
                UnsupportedOperationException.class, NOT_IMPLEMENTED
        );
    }

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        var status = exceptionMap.getOrDefault(exception.getClass(), SERVICE_UNAVAILABLE);
        return Response.status(status).build();
    }
}
