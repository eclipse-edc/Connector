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

package org.eclipse.edc.transform.transformer.edc.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class JsonObjectFromDataAddressTransformer extends AbstractJsonLdTransformer<DataAddress, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDataAddressTransformer(JsonBuilderFactory jsonBuilderFactory, TypeManager mapper, String typeContext) {
        super(DataAddress.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.typeManager = mapper;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataAddress dataAddress, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(TYPE, EDC_NAMESPACE + "DataAddress");

        Function<Object, JsonValue> func = v -> {
            if (v instanceof DataAddress) {
                return context.transform(v, JsonObject.class);
            }
            return typeManager.getMapper(typeContext).convertValue(v, JsonValue.class);
        };

        transformProperties(dataAddress.getProperties(), builder, func, context);

        return builder.build();
    }
}
