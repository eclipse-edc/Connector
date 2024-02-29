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
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_REASON;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_TYPE;


/**
 * Converts from a {@link DataFlowTerminateMessage} to a {@link JsonObject} in JSON-LD expanded form .
 */
public class JsonObjectFromDataFlowTerminateMessageTransformer extends AbstractJsonLdTransformer<DataFlowTerminateMessage, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataFlowTerminateMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(DataFlowTerminateMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataFlowTerminateMessage message, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, DATA_FLOW_TERMINATE_MESSAGE_TYPE);

        Optional.ofNullable(message.getReason()).ifPresent(reason -> builder.add(DATA_FLOW_TERMINATE_MESSAGE_REASON, reason));

        return builder.build();
    }
}
