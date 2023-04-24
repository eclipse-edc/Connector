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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectFromDataAddressTransformer extends AbstractJsonLdTransformer<DataAddress, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromDataAddressTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(DataAddress.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataAddress transferCompletionMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        transferCompletionMessage.getProperties().forEach((k, v) -> builder.add(k, v));

        return builder.build();
    }
}