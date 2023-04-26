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
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CODE_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_REASON_TYPE;

public class JsonObjectToTransferTerminationMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferTerminationMessage> {

    public JsonObjectToTransferTerminationMessageTransformer() {
        super(JsonObject.class, TransferTerminationMessage.class);
    }

    @Override
    public @Nullable TransferTerminationMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var transferTerminationMessageBuilder = TransferTerminationMessage.Builder.newInstance();

        transferTerminationMessageBuilder.protocol(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);

        transformString(jsonObject.get(DSPACE_PROCESSID_TYPE), transferTerminationMessageBuilder::processId, context);

        if (jsonObject.containsKey(DSPACE_CODE_TYPE)) {
            transformString(jsonObject.get(DSPACE_CODE_TYPE), transferTerminationMessageBuilder::code, context);
        }

        var reasons = jsonObject.get(DSPACE_REASON_TYPE);
        if (reasons != null) {
            var result = typeValueArray(reasons, context);
            if (result == null) {
                context.reportProblem(format("Cannot transform property %s in ContractNegotiationTerminationMessage", DSPACE_REASON_TYPE));
            } else {
                if (result.size() > 0) transferTerminationMessageBuilder.reason(String.valueOf(result.get(0)));
            }
        }

        return transferTerminationMessageBuilder.build();

    }
}
