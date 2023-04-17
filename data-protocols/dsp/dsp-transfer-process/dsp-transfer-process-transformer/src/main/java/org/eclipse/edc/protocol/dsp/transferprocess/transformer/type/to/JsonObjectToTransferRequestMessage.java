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

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.*;

public class JsonObjectToTransferRequestMessage extends AbstractJsonLdTransformer<JsonObject, TransferRequestMessage> {

    public JsonObjectToTransferRequestMessage() {
        super(JsonObject.class, TransferRequestMessage.class);
    }

    @Override
    public @Nullable TransferRequestMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var type = nodeType(jsonObject, context);


        if (DSPACE_TRANSFERPROCESS_REQUEST_TYPE.equals(type)) {

            var transferRequestMessageBuilder = TransferRequestMessage.Builder.newInstance();

            transferRequestMessageBuilder.id(nodeId(jsonObject))
                    .protocol(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);

            transformString(jsonObject.get(DSPACE_CONTRACTAGREEMENT_TYPE), transferRequestMessageBuilder::contractId, context);
            transformString(jsonObject.get(DSPACE_CALLBACKADDRESS_TYPE), transferRequestMessageBuilder::connectorAddress, context); //TODO Create external Constant and add correct connectorAddress

            transferRequestMessageBuilder.dataDestination(createDataAddress(jsonObject, context));

            return transferRequestMessageBuilder.build();
        } else {
            context.reportProblem(format("Cannot transform type %s to TransferRequestMessage", type));
            return null;
        }
    }

    private DataAddress createDataAddress(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var dataAddressBuilder = DataAddress.Builder.newInstance();  //TODO Add missing properties

        transformString(jsonObject.get(DCT_FORMAT), dataAddressBuilder::type, context);

        // TODO Add dataAddress

        return dataAddressBuilder.build();
    }
}
