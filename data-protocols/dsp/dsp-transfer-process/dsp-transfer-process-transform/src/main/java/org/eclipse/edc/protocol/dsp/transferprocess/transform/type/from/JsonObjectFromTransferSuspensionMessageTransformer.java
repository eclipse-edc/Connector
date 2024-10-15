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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE;

public class JsonObjectFromTransferSuspensionMessageTransformer extends AbstractJsonLdTransformer<TransferSuspensionMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferSuspensionMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferSuspensionMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferSuspensionMessage message, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder()
                .add(ID, message.getId())
                .add(TYPE, DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID_IRI, message.getConsumerPid())
                .add(DSPACE_PROPERTY_PROVIDER_PID_IRI, message.getProviderPid())
                .add(DSPACE_PROPERTY_REASON_IRI, jsonBuilderFactory.createArrayBuilder(message.getReason()));

        addIfNotNull(message.getCode(), DSPACE_PROPERTY_CODE_IRI, builder);

        return builder.build();

    }
}
