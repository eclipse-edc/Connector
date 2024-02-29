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
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage.Builder;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage.DATA_FLOW_SUSPEND_MESSAGE_REASON;

/**
 * Converts from a {@link JsonObject} in JSON-LD expanded form to a {@link DataFlowSuspendMessage}.
 */
public class JsonObjectToDataFlowSuspendMessageTransformer extends AbstractJsonLdTransformer<JsonObject, DataFlowSuspendMessage> {

    public JsonObjectToDataFlowSuspendMessageTransformer() {
        super(JsonObject.class, DataFlowSuspendMessage.class);
    }

    @Override
    public @Nullable DataFlowSuspendMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Builder.newInstance();
        transformString(object.get(DATA_FLOW_SUSPEND_MESSAGE_REASON), builder::reason, context);
        return builder.build();
    }

}
