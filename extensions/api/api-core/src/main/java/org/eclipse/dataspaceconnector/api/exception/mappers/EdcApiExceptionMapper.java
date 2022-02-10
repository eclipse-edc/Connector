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
 *
 */

package org.eclipse.dataspaceconnector.api.exception.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.eclipse.dataspaceconnector.api.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.api.exception.NotAuthorizedException;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotModifiableException;

import java.util.Map;

public class EdcApiExceptionMapper implements ExceptionMapper<Throwable> {
    private final Map<Class<? extends Throwable>, Integer> exceptionMap;

    public EdcApiExceptionMapper() {
        exceptionMap = Map.of(
                IllegalArgumentException.class, 400,
                NullPointerException.class, 400,
                AuthenticationFailedException.class, 401,
                NotAuthorizedException.class, 403,
                ObjectNotFoundException.class, 404,
                ObjectExistsException.class, 409,
                ObjectNotModifiableException.class, 423,
                UnsupportedOperationException.class, 501
        );
    }

    @Override
    public Response toResponse(Throwable exception) {
        var code = exceptionMap.get(exception.getClass());
        if (code == null) {
            return Response.status(503).build();
        } else {
            return Response.status(code).entity(exception.getMessage()).build();
        }
    }
}
