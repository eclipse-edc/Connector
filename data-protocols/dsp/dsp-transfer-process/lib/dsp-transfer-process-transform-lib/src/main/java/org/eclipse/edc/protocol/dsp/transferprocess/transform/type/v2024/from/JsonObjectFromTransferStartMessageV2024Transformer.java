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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;

public class JsonObjectFromTransferStartMessageV2024Transformer extends AbstractNamespaceAwareJsonLdTransformer<TransferStartMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferStartMessageV2024Transformer(JsonBuilderFactory jsonBuilderFactory) {
        this(jsonBuilderFactory, DSP_NAMESPACE_V_2024_1);
    }

    public JsonObjectFromTransferStartMessageV2024Transformer(JsonBuilderFactory jsonBuilderFactory, JsonLdNamespace namespace) {
        super(TransferStartMessage.class, JsonObject.class, namespace);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferStartMessage transferStartMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferStartMessage.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), createId(jsonBuilderFactory, transferStartMessage.getProviderPid()))
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), createId(jsonBuilderFactory, transferStartMessage.getConsumerPid()));

        if (transferStartMessage.getDataAddress() != null) {
            builder.add(forNamespace(DSPACE_PROPERTY_DATA_ADDRESS_TERM), context.transform(transferStartMessage.getDataAddress(), JsonObject.class));
        }

        return builder.build();
    }
}
