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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;

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

        if (!transformMandatoryString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context)) {
            return null;
        }

        var code = object.get(DSPACE_NEGOTIATION_PROPERTY_CODE);
        if (code != null) { // optional property
            transformString(code, builder::code, context);
        }

        var reasons = object.get(DSPACE_NEGOTIATION_PROPERTY_REASON);
        if (reasons != null) {  // optional property
            if (reasons instanceof JsonArray) {
                var array = (JsonArray) reasons;
                if (array.size() > 0) {
                    builder.rejectionReason(array.toString());
                }
            } else {
                context.problem()
                        .unexpectedType()
                        .type(DSPACE_NEGOTIATION_TERMINATION_MESSAGE)
                        .property(DSPACE_NEGOTIATION_PROPERTY_REASON)
                        .actual(reasons.getValueType().toString())
                        .expected(ARRAY)
                        .report();
            }
        }

        return builder.build();
    }
}
