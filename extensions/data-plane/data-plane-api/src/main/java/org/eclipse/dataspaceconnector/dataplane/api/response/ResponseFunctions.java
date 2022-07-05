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
import org.eclipse.dataspaceconnector.dataplane.spi.response.TransferErrorResponse;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.status;

/**
 * Helpers for handling responses
 */
public final class ResponseFunctions {

    /**
     * Returns a response for a collection of authentication errors.
     *
     * @param errors List of errors.
     * @return Error response.
     */
    public static Response notAuthorizedErrors(List<String> errors) {
        return createErrorResponse(FORBIDDEN, errors);
    }

    /**
     * Returns a response for a collection of validation errors.
     *
     * @param errors List of errors.
     * @return Error response.
     */
    public static Response validationErrors(List<String> errors) {
        return createErrorResponse(BAD_REQUEST, errors);
    }

    /**
     * Returns a response for a collection of internal errors.
     *
     * @param errors List of errors.
     * @return Error response.
     */
    public static Response internalErrors(List<String> errors) {
        return createErrorResponse(INTERNAL_SERVER_ERROR, errors);
    }

    /**
     * Returns a response for a unique validation error.
     *
     * @param error Error description.
     * @return Error response.
     */
    public static Response validationError(String error) {
        return validationErrors(List.of(error));
    }

    /**
     * Returns a sucess response containing the data retrieved from the data source.
     *
     * @param data Data retrieved from the data source.
     * @return Success response containing the data.
     */
    public static Response success(String data) {
        return Response.ok().entity(data).build();
    }

    private static Response createErrorResponse(Response.Status status, List<String> errors) {
        return status(status).entity(new TransferErrorResponse(errors)).build();
    }

    private ResponseFunctions() {
    }
}
