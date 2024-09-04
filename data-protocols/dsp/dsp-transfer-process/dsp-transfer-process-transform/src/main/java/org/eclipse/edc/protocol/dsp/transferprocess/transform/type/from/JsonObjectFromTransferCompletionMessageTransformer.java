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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE;


public class JsonObjectFromTransferCompletionMessageTransformer extends AbstractJsonLdTransformer<TransferCompletionMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferCompletionMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferCompletionMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferCompletionMessage transferCompletionMessage, @NotNull TransformerContext context) {
        return jsonBuilderFactory.createObjectBuilder()
                .add(ID, transferCompletionMessage.getId())
                .add(TYPE, DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, transferCompletionMessage.getConsumerPid())
                .add(DSPACE_PROPERTY_PROVIDER_PID, transferCompletionMessage.getProviderPid())
                .build();
    }
}
