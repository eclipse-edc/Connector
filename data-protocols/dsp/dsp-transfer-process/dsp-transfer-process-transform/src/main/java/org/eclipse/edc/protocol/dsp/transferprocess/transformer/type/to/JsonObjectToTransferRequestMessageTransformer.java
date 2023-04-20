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
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CONTRACTAGREEMENT_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATAADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;

public class JsonObjectToTransferRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferRequestMessage> {

    public JsonObjectToTransferRequestMessageTransformer() {
        super(JsonObject.class, TransferRequestMessage.class);
    }

    @Override
    public @Nullable TransferRequestMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var transferRequestMessageBuilder = TransferRequestMessage.Builder.newInstance();

        transferRequestMessageBuilder.id(nodeId(jsonObject))
                .protocol(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);

        transformString(jsonObject.get(DSPACE_CONTRACTAGREEMENT_TYPE), transferRequestMessageBuilder::contractId, context);
        transformString(jsonObject.get(DSPACE_CALLBACKADDRESS_TYPE), transferRequestMessageBuilder::callbackAddress, context);

        transferRequestMessageBuilder.dataDestination(createDataAddress(jsonObject, context));

        return transferRequestMessageBuilder.build();
    }

    private DataAddress createDataAddress(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var dataAddressBuilder = DataAddress.Builder.newInstance();

        transformString(jsonObject.get(DCT_FORMAT_ATTRIBUTE), dataAddressBuilder::type, context);

        if (jsonObject.containsKey(DSPACE_DATAADDRESS_TYPE)) {
            var dataAddressJsonObject = jsonObject.getJsonObject(DSPACE_DATAADDRESS_TYPE);

            dataAddressJsonObject.entrySet().forEach((k) -> dataAddressBuilder.property(k.getKey(), dataAddressJsonObject.getString(k.getKey())));
        }

        return dataAddressBuilder.build();
    }
}
