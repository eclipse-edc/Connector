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
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_TRANSFER_START_TYPE;

public class JsonObjectToTransferStartMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferStartMessage> {

    public JsonObjectToTransferStartMessageTransformer() {
        super(JsonObject.class, TransferStartMessage.class);
    }

    @Override
    public @Nullable TransferStartMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var type = nodeType(jsonObject, context);


        if (DSPACE_TRANSFER_START_TYPE.equals(type)) {

            var transferStartMessageBuilder = TransferStartMessage.Builder.newInstance();

            transferStartMessageBuilder
                    .protocol(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);

            transformString(jsonObject.get(DSPACE_PROCESSID_TYPE), transferStartMessageBuilder::processId, context);
            transformString(jsonObject.get(DSPACE_CALLBACKADDRESS_TYPE), transferStartMessageBuilder::callbackAddress, context);
            //TODO ADD missing fields dataAddress from spec

            return transferStartMessageBuilder.build();
        } else {
            context.reportProblem(format("Cannot transform type %s to TransferRequestMessage", type));
            return null;
        }
    }
}
