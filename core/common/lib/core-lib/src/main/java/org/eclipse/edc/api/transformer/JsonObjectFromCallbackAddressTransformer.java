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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.AUTH_CODE_ID;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;

public class JsonObjectFromCallbackAddressTransformer extends AbstractJsonLdTransformer<CallbackAddress, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromCallbackAddressTransformer(JsonBuilderFactory jsonFactory) {
        super(CallbackAddress.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CallbackAddress callbackAddress, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, CALLBACKADDRESS_TYPE)
                .add(IS_TRANSACTIONAL, callbackAddress.isTransactional())
                .add(URI, callbackAddress.getUri())
                .add(EVENTS, asArray(callbackAddress.getEvents()));

        ofNullable(callbackAddress.getAuthKey()).ifPresent((authKey) -> builder.add(AUTH_KEY, authKey));
        ofNullable(callbackAddress.getAuthCodeId()).ifPresent((authCodeId) -> builder.add(AUTH_CODE_ID, authCodeId));

        return builder.build();
    }

    private JsonArrayBuilder asArray(Set<String> events) {
        var bldr = jsonFactory.createArrayBuilder();
        events.forEach(bldr::add);
        return bldr;
    }
}
