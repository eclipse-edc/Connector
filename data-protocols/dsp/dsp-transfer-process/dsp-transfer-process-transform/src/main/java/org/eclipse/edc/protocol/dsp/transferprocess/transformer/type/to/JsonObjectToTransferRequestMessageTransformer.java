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
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESS_ID;

public class JsonObjectToTransferRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferRequestMessage> {

    public JsonObjectToTransferRequestMessageTransformer() {
        super(JsonObject.class, TransferRequestMessage.class);
    }

    @Override
    public @Nullable TransferRequestMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var transferRequestMessageBuilder = TransferRequestMessage.Builder.newInstance();

        visitProperties(messageObject, k -> {
            switch (k) {
                case DSPACE_PROCESS_ID: return v -> transferRequestMessageBuilder.processId(transformString(v, context));
                case DSPACE_CONTRACT_AGREEMENT_ID: return v -> transferRequestMessageBuilder.contractId(transformString(v, context));
                case DSPACE_CALLBACK_ADDRESS: return v -> transferRequestMessageBuilder.callbackAddress(transformString(v, context));
                default: return doNothing();
            }
        });

        transferRequestMessageBuilder.dataDestination(createDataAddress(messageObject, context));

        return transferRequestMessageBuilder.build();
    }

    // TODO replace with JsonObjectToDataAddressTransformer
    private DataAddress createDataAddress(@NotNull JsonObject requestObject, @NotNull TransformerContext context) {
        var dataAddressBuilder = DataAddress.Builder.newInstance();

        transformString(requestObject.get(DCT_FORMAT_ATTRIBUTE), dataAddressBuilder::type, context);

        var dataAddressObject = returnJsonObject(requestObject.get(DSPACE_DATA_ADDRESS), context, DSPACE_DATA_ADDRESS, false);
        if (dataAddressObject != null) {
            dataAddressObject.forEach((key, value) -> transformString(value, v -> dataAddressBuilder.property(key, v), context));
        }

        return dataAddressBuilder.build();
    }
}
