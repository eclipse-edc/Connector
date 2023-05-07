/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;

public class JsonObjectToCallbackAddressDtoTransformer extends AbstractJsonLdTransformer<JsonObject, CallbackAddressDto> {

    public JsonObjectToCallbackAddressDtoTransformer() {
        super(JsonObject.class, CallbackAddressDto.class);
    }

    @Override
    public @Nullable CallbackAddressDto transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = CallbackAddressDto.Builder.newInstance();

        visitProperties(jsonObject, (key, value) -> setProperties(key, value, builder, context));

        return builder.build();
    }

    private void setProperties(String key, JsonValue value, CallbackAddressDto.Builder builder, TransformerContext context) {
        switch (key) {
            case IS_TRANSACTIONAL:
                builder.transactional(Boolean.parseBoolean(value.toString()));
                break;
            case URI:
                transformString(value, builder::uri, context);
                break;
            case EVENTS:
                var evt = new HashSet<String>();
                transformArrayOrObject(value, String.class, evt::add, context);
                builder.events(evt);
                break;
            default:
                context.reportProblem("Cannot convert key " + key + " as it is not known");
                break;
        }
    }
}
