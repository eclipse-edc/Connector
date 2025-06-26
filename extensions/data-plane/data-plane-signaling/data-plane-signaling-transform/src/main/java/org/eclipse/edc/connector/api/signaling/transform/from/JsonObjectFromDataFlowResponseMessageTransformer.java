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

package org.eclipse.edc.connector.api.signaling.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_PROVISIONING;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_TYPE;

/**
 * Converts from a {@link DataFlowResponseMessage} to a {@link JsonObject} in JSON-LD expanded form .
 */
public class JsonObjectFromDataFlowResponseMessageTransformer extends AbstractJsonLdTransformer<DataFlowResponseMessage, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataFlowResponseMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(DataFlowResponseMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataFlowResponseMessage message, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, DATA_FLOW_RESPONSE_MESSAGE_TYPE)
                .add(DATA_FLOW_RESPONSE_MESSAGE_PROVISIONING, message.isProvisioning());

        Optional.ofNullable(message.getDataAddress())
                .ifPresent(dataAddress -> builder.add(DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS, context.transform(message.getDataAddress(), JsonObject.class)));
        return builder.build();
    }
}
