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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CODE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_REASON;

public class JsonObjectToTransferTerminationMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferTerminationMessage> {

    public JsonObjectToTransferTerminationMessageTransformer() {
        super(JsonObject.class, TransferTerminationMessage.class);
    }

    @Override
    public @Nullable TransferTerminationMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var transferTerminationMessageBuilder = TransferTerminationMessage.Builder.newInstance();

        transformString(messageObject.get(DSPACE_PROCESS_ID), transferTerminationMessageBuilder::processId, context);

        if (messageObject.containsKey(DSPACE_CODE)) {
            transformString(messageObject.get(DSPACE_CODE), transferTerminationMessageBuilder::code, context);
        }

        var reasons = messageObject.get(DSPACE_REASON);
        if (reasons != null) {  // optional property
            var result = typeValueArray(reasons, context);
            if (result == null) {
                context.reportProblem(format("Cannot transform property %s in ContractNegotiationTerminationMessage", DSPACE_REASON));
            } else {
                if (result.size() > 0) {
                    transferTerminationMessageBuilder.reason(result.toString());
                }
            }
        }

        return transferTerminationMessageBuilder.build();

    }
}
