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

package org.eclipse.edc.api.transformer;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.AUTH_CODE_ID;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;

public class JsonObjectToCallbackAddressTransformer extends AbstractJsonLdTransformer<JsonObject, CallbackAddress> {

    public JsonObjectToCallbackAddressTransformer() {
        super(JsonObject.class, CallbackAddress.class);
    }

    @Override
    public @Nullable CallbackAddress transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = CallbackAddress.Builder.newInstance();

        visitProperties(jsonObject, (key, value) -> setProperties(key, value, builder, context));

        return builder.build();
    }

    private void setProperties(String key, JsonValue value, CallbackAddress.Builder builder, TransformerContext context) {
        switch (key) {
            case IS_TRANSACTIONAL:
                builder.transactional(transformBoolean(value, context));
                break;
            case URI:
                transformString(value, builder::uri, context);
                break;
            case EVENTS:
                var evt = new HashSet<String>();
                visitArray(value, v -> evt.add(transformString(v, context)), context);
                builder.events(evt);
                break;
            case AUTH_KEY:
                transformString(value, builder::authKey, context);
                break;
            case AUTH_CODE_ID:
                transformString(value, builder::authCodeId, context);
                break;
            default:
                context.problem()
                        .unexpectedType()
                        .type("CallbackAddress")
                        .property(key)
                        .expected(IS_TRANSACTIONAL)
                        .expected(URI)
                        .expected(EVENTS)
                        .expected(AUTH_KEY)
                        .expected(AUTH_CODE_ID)
                        .report();
                break;
        }
    }
}
