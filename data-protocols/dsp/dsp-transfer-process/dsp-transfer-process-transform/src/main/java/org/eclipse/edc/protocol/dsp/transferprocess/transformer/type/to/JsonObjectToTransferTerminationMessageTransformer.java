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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE;

public class JsonObjectToTransferTerminationMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferTerminationMessage> {

    public JsonObjectToTransferTerminationMessageTransformer() {
        super(JsonObject.class, TransferTerminationMessage.class);
    }

    @Override
    public @Nullable TransferTerminationMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var transferTerminationMessageBuilder = TransferTerminationMessage.Builder.newInstance();

        if (!transformMandatoryString(messageObject.get(DSPACE_PROPERTY_PROCESS_ID), transferTerminationMessageBuilder::processId, context)) {
            return null;
        }

        if (messageObject.containsKey(DSPACE_PROPERTY_CODE)) {
            transformString(messageObject.get(DSPACE_PROPERTY_CODE), transferTerminationMessageBuilder::code, context);
        }

        var reasons = messageObject.get(DSPACE_PROPERTY_REASON);
        if (reasons != null) {  // optional property
            if (!(reasons instanceof JsonArray)) {
                context.problem()
                        .unexpectedType()
                        .type(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE)
                        .property(DSPACE_PROPERTY_REASON)
                        .actual(reasons.getValueType())
                        .expected(ARRAY)
                        .report();
            } else {
                var array = (JsonArray) reasons;
                if (array.size() > 0) {
                    transferTerminationMessageBuilder.reason(array.toString());
                }
            }
        }

        return transferTerminationMessageBuilder.build();

    }
}
