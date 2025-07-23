/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;


public class JsonObjectFromTransferRequestMessageV2024Transformer extends AbstractNamespaceAwareJsonLdTransformer<TransferRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferRequestMessageV2024Transformer(JsonBuilderFactory jsonBuilderFactory, JsonLdNamespace namespace) {
        super(TransferRequestMessage.class, JsonObject.class, namespace);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferRequestMessage transferRequestMessage, @NotNull TransformerContext context) {
        var format = Optional.ofNullable(transferRequestMessage.getTransferType())
                .orElseGet(() -> transferRequestMessage.getDataDestination().getType());

        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferRequestMessage.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM), createId(jsonBuilderFactory, transferRequestMessage.getContractId()))
                .add(DCT_FORMAT_ATTRIBUTE, createId(jsonBuilderFactory, format))
                .add(forNamespace(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), transferRequestMessage.getCallbackAddress())
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), createId(jsonBuilderFactory, transferRequestMessage.getConsumerPid()));

        if (transferRequestMessage.getDataDestination() != null && transferRequestMessage.getDataDestination().getProperties().size() > 1) {
            builder.add(forNamespace(DSPACE_PROPERTY_DATA_ADDRESS_TERM), context.transform(transferRequestMessage.getDataDestination(), JsonObject.class));
        }
        return builder.build();
    }
}
