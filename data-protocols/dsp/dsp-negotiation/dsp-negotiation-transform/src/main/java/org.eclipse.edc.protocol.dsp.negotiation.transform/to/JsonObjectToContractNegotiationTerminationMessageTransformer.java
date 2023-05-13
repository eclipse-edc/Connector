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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractNegotiationTerminationMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractNegotiationTerminationMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractNegotiationTerminationMessage> {

    public JsonObjectToContractNegotiationTerminationMessageTransformer() {
        super(JsonObject.class, ContractNegotiationTerminationMessage.class);
    }

    @Override
    public @Nullable ContractNegotiationTerminationMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractNegotiationTerminationMessage.Builder.newInstance();

        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context);

        var code = object.get(DSPACE_NEGOTIATION_PROPERTY_CODE);
        if (code != null) { // optional property
            transformString(code, builder::code, context);
        }

        var reasons = object.get(DSPACE_NEGOTIATION_PROPERTY_REASON);
        if (reasons != null) {  // optional property
            var result = typeValueArray(reasons, context);
            if (result == null) {
                context.reportProblem(format("Cannot transform property %s in ContractNegotiationTerminationMessage", DSPACE_NEGOTIATION_PROPERTY_REASON));
            } else {
                if (result.size() > 0) {
                    builder.rejectionReason(result.toString());
                }
            }
        }

        return builder.build();
    }
}
