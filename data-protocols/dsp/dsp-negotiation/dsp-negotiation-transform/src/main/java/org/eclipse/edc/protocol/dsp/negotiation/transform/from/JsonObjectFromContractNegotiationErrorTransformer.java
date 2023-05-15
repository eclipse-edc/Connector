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

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.ContractNegotiationError;
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION_ERROR;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;

public class JsonObjectFromContractNegotiationErrorTransformer extends AbstractJsonLdTransformer<ContractNegotiationError, JsonObject> {

    private final DspHttpStatusCodeMapper statusCodeMapper;

    public JsonObjectFromContractNegotiationErrorTransformer(DspHttpStatusCodeMapper statusCodeMapper) {
        super(ContractNegotiationError.class, JsonObject.class);
        this.statusCodeMapper = statusCodeMapper;
    }

    @Nullable
    @Override
    public JsonObject transform(@NotNull ContractNegotiationError error, @NotNull TransformerContext context) {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION_ERROR);

        error.getProcessId().map(e -> builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, e))
                .orElseGet(() -> builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, "InvalidId"));

        var exception = error.getException();

        builder.add(DSPACE_NEGOTIATION_PROPERTY_CODE, String.valueOf(statusCodeMapper.mapErrorToStatusCode(exception)));

        if (exception.getMessage() != null) {
            builder.add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createArrayBuilder().add(exception.getMessage()));
        }

        return builder.build();
    }
}
