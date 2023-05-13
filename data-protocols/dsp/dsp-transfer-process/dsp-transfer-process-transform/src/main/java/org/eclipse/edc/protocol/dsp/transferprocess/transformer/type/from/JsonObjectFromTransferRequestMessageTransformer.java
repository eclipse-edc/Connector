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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_PROCESS_REQUEST_TYPE;


public class JsonObjectFromTransferRequestMessageTransformer extends AbstractJsonLdTransformer<TransferRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferRequestMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferRequestMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }


    @Override
    public @Nullable JsonObject transform(@NotNull TransferRequestMessage transferRequestMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(JsonLdKeywords.ID, transferRequestMessage.getId());
        builder.add(JsonLdKeywords.TYPE, DSPACE_TRANSFER_PROCESS_REQUEST_TYPE);

        builder.add(DSPACE_CONTRACT_AGREEMENT_ID, transferRequestMessage.getContractId());
        builder.add(DCT_FORMAT_ATTRIBUTE, transferRequestMessage.getDataDestination().getType());
        builder.add(DSPACE_CALLBACK_ADDRESS, transferRequestMessage.getCallbackAddress());
        builder.add(DSPACE_PROCESS_ID, transferRequestMessage.getProcessId());

        if (transferRequestMessage.getDataDestination().getProperties().size() > 1) {
            builder.add(DSPACE_DATA_ADDRESS, context.transform(transferRequestMessage.getDataDestination(), JsonObject.class));
        }
        return builder.build();
    }
}
