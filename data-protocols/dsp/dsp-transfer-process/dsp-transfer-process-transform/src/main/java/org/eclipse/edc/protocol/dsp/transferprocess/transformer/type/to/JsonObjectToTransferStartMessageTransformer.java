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
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATAADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;

public class JsonObjectToTransferStartMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferStartMessage> {

    public JsonObjectToTransferStartMessageTransformer() {
        super(JsonObject.class, TransferStartMessage.class);
    }

    @Override
    public @Nullable TransferStartMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var transferStartMessageBuilder = TransferStartMessage.Builder.newInstance();

        transferStartMessageBuilder.protocol(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);

        transformString(jsonObject.get(DSPACE_PROCESSID_TYPE), transferStartMessageBuilder::processId, context);

        if (jsonObject.containsKey(DSPACE_DATAADDRESS_TYPE)) {
            if (!jsonObject.get(DSPACE_DATAADDRESS_TYPE).asJsonObject().isEmpty()) {
                transferStartMessageBuilder.dataAddress(context.transform(jsonObject.get(DSPACE_DATAADDRESS_TYPE), DataAddress.class));
            }
        }

        return transferStartMessageBuilder.build();
    }
}
