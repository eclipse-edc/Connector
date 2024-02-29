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

package org.eclipse.edc.core.transform.transformer.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage.Builder;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_REASON;

/**
 * Converts from a {@link JsonObject} in JSON-LD expanded form to a {@link DataFlowTerminateMessage}.
 */
public class JsonObjectToDataFlowTerminateMessageTransformer extends AbstractJsonLdTransformer<JsonObject, DataFlowTerminateMessage> {

    public JsonObjectToDataFlowTerminateMessageTransformer() {
        super(JsonObject.class, DataFlowTerminateMessage.class);
    }

    @Override
    public @Nullable DataFlowTerminateMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Builder.newInstance();
        transformString(object.get(DATA_FLOW_TERMINATE_MESSAGE_REASON), builder::reason, context);
        return builder.build();
    }

}
