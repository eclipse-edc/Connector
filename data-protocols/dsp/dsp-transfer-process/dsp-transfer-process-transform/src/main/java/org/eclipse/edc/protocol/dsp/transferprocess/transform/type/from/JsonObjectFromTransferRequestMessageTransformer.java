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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE;


public class JsonObjectFromTransferRequestMessageTransformer extends AbstractJsonLdTransformer<TransferRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferRequestMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferRequestMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferRequestMessage transferRequestMessage, @NotNull TransformerContext context) {
        var format = Optional.ofNullable(transferRequestMessage.getTransferType())
                .orElseGet(() -> transferRequestMessage.getDataDestination().getType());

        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferRequestMessage.getId())
                .add(TYPE, DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE)
                .add(DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID, transferRequestMessage.getContractId())
                .add(DCT_FORMAT_ATTRIBUTE, format)
                .add(DSPACE_PROPERTY_CALLBACK_ADDRESS, transferRequestMessage.getCallbackAddress())
                .add(DSPACE_PROPERTY_CONSUMER_PID, transferRequestMessage.getConsumerPid());

        if (transferRequestMessage.getDataDestination() != null && transferRequestMessage.getDataDestination().getProperties().size() > 1) {
            builder.add(DSPACE_PROPERTY_DATA_ADDRESS, context.transform(transferRequestMessage.getDataDestination(), JsonObject.class));
        }
        return builder.build();
    }
}
