/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.TransferError;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CODE_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_REASON_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_PROCESS_ERROR;

public class JsonObjectFromTransferErrorTransformer extends AbstractJsonLdTransformer<TransferError, JsonObject> {
    public JsonObjectFromTransferErrorTransformer() {
        super(TransferError.class, JsonObject.class);
    }

    @Nullable
    @Override
    public JsonObject transform(@NotNull TransferError error, @NotNull TransformerContext context) {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_TRANSFER_PROCESS_ERROR);

        error.getProcessId().map(e -> builder.add(DSPACE_PROCESSID_TYPE, e))
                .orElseGet(() -> builder.add(DSPACE_PROCESSID_TYPE, "InvalidId"));

        var throwable = error.getThrowable();

        var code = errorCodeMapping(throwable);

        builder.add(DSPACE_CODE_TYPE, String.valueOf(code));

        if (throwable.getMessage() != null) {
            builder.add(DSPACE_REASON_TYPE, Json.createArrayBuilder().add(throwable.getMessage()));
        }

        return builder.build();
    }

    private static int errorCodeMapping(Throwable throwable) {
        var exceptionMap = Map.of(
                AuthenticationFailedException.class, UNAUTHORIZED,
                NotAuthorizedException.class, FORBIDDEN,
                InvalidRequestException.class, BAD_REQUEST,
                ObjectNotFoundException.class, NOT_FOUND,
                ObjectConflictException.class, CONFLICT,
                BadGatewayException.class, BAD_GATEWAY,
                UnsupportedOperationException.class, NOT_IMPLEMENTED
        );

        var status = exceptionMap.getOrDefault(throwable.getClass(), INTERNAL_SERVER_ERROR);

        return status.getStatusCode();
    }
}
