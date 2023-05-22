/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.api.transformer;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectFromDataAddressDtoTransformer extends AbstractJsonLdTransformer<DataAddressDto, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromDataAddressDtoTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(DataAddressDto.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataAddressDto dataAddress, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        dataAddress.getProperties().forEach(builder::add);

        return builder.build();
    }
}
