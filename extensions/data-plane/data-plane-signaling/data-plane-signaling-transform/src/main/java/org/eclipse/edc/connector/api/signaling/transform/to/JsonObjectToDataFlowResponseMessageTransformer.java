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

package org.eclipse.edc.connector.api.signaling.transform.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_PROVISIONING;

/**
 * Converts from a {@link JsonObject} in JSON-LD expanded form to a {@link DataFlowResponseMessage}.
 */
public class JsonObjectToDataFlowResponseMessageTransformer extends AbstractJsonLdTransformer<JsonObject, DataFlowResponseMessage> {

    public JsonObjectToDataFlowResponseMessageTransformer() {
        super(JsonObject.class, DataFlowResponseMessage.class);
    }

    @Override
    public @Nullable DataFlowResponseMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = DataFlowResponseMessage.Builder.newInstance()
                .provisioning(transformBoolean(object.get(DATA_FLOW_RESPONSE_MESSAGE_PROVISIONING), context));

        Optional.ofNullable(object.get(DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS))
                .ifPresent(jsonValue -> builder.dataAddress(transformObject(jsonValue, DataAddress.class, context)));
        
        return builder.build();
    }
}
