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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey.mapper;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.dataspaceconnector.spi.ApiErrorDetail;

import java.util.Objects;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * Exception mapper that catches all the `ConstraintViolationException` exceptions thrown by the `jersey-bean-validation` module,
 * it maps them to a 400 response code with a detailed response body
 */
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var errors = exception.getConstraintViolations().stream()
                .map(violation -> ApiErrorDetail.Builder.newInstance()
                        .message(violation.getMessage())
                        .type(violation.getMessageTemplate())
                        .path(violation.getPropertyPath().toString())
                        .value(Objects.toString(violation.getInvalidValue()))
                        .build())
                .collect(Collectors.toList());

        return Response
                .status(BAD_REQUEST)
                .entity(errors)
                .build();
    }
}
