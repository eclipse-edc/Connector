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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI;

public class JsonObjectFromTransferStartMessageTransformer extends AbstractJsonLdTransformer<TransferStartMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferStartMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferStartMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferStartMessage transferStartMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferStartMessage.getId())
                .add(TYPE, DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI)
                .add(DSPACE_PROPERTY_PROVIDER_PID_IRI, transferStartMessage.getProviderPid())
                .add(DSPACE_PROPERTY_CONSUMER_PID_IRI, transferStartMessage.getConsumerPid());

        if (transferStartMessage.getDataAddress() != null) {
            builder.add(DSPACE_PROPERTY_DATA_ADDRESS, context.transform(transferStartMessage.getDataAddress(), JsonObject.class));
        }

        return builder.build();
    }
}
