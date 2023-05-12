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
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.TransferError;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CODE_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_REASON_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_PROCESS_ERROR;

public class JsonObjectFromTransferErrorTransformer extends AbstractJsonLdTransformer<TransferError, JsonObject> {

    private DspHttpStatusCodeMapper statusCodeMapper;

    public JsonObjectFromTransferErrorTransformer(DspHttpStatusCodeMapper statusCodeMapper) {
        super(TransferError.class, JsonObject.class);
        this.statusCodeMapper = statusCodeMapper;
    }

    @Nullable
    @Override
    public JsonObject transform(@NotNull TransferError error, @NotNull TransformerContext context) {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_TRANSFER_PROCESS_ERROR);

        error.getProcessId().map(e -> builder.add(DSPACE_PROCESSID_TYPE, e))
                .orElseGet(() -> builder.add(DSPACE_PROCESSID_TYPE, "InvalidId"));

        var exception = error.getException();

        builder.add(DSPACE_CODE_TYPE, String.valueOf(statusCodeMapper.mapErrorToStatusCode(exception)));

        if (exception.getMessage() != null) {
            builder.add(DSPACE_REASON_TYPE, Json.createArrayBuilder().add(exception.getMessage()));
        }

        return builder.build();
    }
}
