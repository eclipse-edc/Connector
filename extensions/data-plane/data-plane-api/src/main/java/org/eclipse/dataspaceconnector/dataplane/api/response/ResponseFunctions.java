/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.api.response;

import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.status;

/**
 * Helpers for handling responses
 */
public final class ResponseFunctions {

    private static final String ERRORS = "errors";

    /**
     * Returns a response for a collection of authentication errors.
     */
    public static Response notAuthorizedErrors(List<String> errors) {
        return status(UNAUTHORIZED).entity(Map.of(ERRORS, errors)).build();
    }

    /**
     * Returns a response for a collection of validation errors.
     */
    public static Response validationErrors(List<String> errors) {
        return status(BAD_REQUEST).entity(Map.of(ERRORS, errors)).build();
    }

    /**
     * Returns a response for a collection of internal errors.
     */
    public static Response internalErrors(List<String> errors) {
        return status(INTERNAL_SERVER_ERROR).entity(Map.of(ERRORS, errors)).build();
    }

    /**
     * Returns a response for a validation error.
     */
    public static Response validationError(String error) {
        return validationErrors(List.of(error));
    }

    /**
     * Returns a successful response containing the requested data.
     */
    public static Response success(String data) {
        return Response.ok().entity(data).build();
    }

    private ResponseFunctions() {
    }
}
