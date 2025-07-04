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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


/**
 * Converts from an {@link DataAddress} as a {@link JsonObject} in JSON-LD expanded form to an {@link DataAddress}.
 */
public class JsonObjectToDataAddressTransformer extends AbstractJsonLdTransformer<JsonObject, DataAddress> {

    //TODO: move into a module-level constants file
    public static final String PROPERTIES_KEY = EDC_NAMESPACE + "properties";

    public JsonObjectToDataAddressTransformer() {
        super(JsonObject.class, DataAddress.class);
    }

    @Override
    public @Nullable DataAddress transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataAddress.Builder.newInstance();
        visitProperties(jsonObject, (s, v) -> transformProperties(s, v, builder, context));
        return builder.build();
    }

    private void transformProperties(String key, JsonValue jsonValue, DataAddress.Builder builder, TransformerContext context) {
        var firstValue = returnJsonObject(jsonValue, context, key, true);

        if (firstValue != null && key.equals(PROPERTIES_KEY)) {
            visitProperties(firstValue, (k, val) -> transformProperties(k, val, builder, context));
        } else if (firstValue != null && key.equals(DataAddress.EDC_DATA_ADDRESS_RESPONSE_CHANNEL)) {
            builder.responseChannel(transformObject(firstValue, DataAddress.class, context));

        } else {
            builder.property(key, transformGenericProperty(jsonValue, context));
        }

    }


}
