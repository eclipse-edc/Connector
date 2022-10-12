/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

/**
 * Exception mapper that catches all the exceptions that are not handled by the other mappers, for example:
 * - all the java.lang exceptions
 * - jakarta.ws.rs.WebApplicationException and subclasses
 */
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private final Monitor monitor;
    private final Map<Class<? extends Throwable>, Response.Status> exceptionMap = Map.of(
            UnsupportedOperationException.class, NOT_IMPLEMENTED
    );

    public UnexpectedExceptionMapper(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Response toResponse(Throwable exception) {
        monitor.severe("JerseyExtension: Unexpected exception caught", exception);
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        var status = exceptionMap.getOrDefault(exception.getClass(), INTERNAL_SERVER_ERROR);

        return Response.status(status).build();
    }
}
