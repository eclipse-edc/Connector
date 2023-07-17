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

package org.eclipse.edc.connector.dataplane.selector.transformer;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.api.SelectionRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToSelectionRequestTransformer extends AbstractJsonLdTransformer<JsonObject, SelectionRequest> {

    public JsonObjectToSelectionRequestTransformer() {
        super(JsonObject.class, SelectionRequest.class);

    }

    @Override
    public @Nullable SelectionRequest transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {

        var builder = SelectionRequest.Builder.newInstance();

        visitProperties(jsonObject, (key, jsonValue) -> {
            switch (key) {
                case SelectionRequest.DEST_ADDRESS ->
                        builder.destination(transformObject(jsonValue, DataAddress.class, context));
                case SelectionRequest.SOURCE_ADDRESS ->
                        builder.source(transformObject(jsonValue, DataAddress.class, context));
                case SelectionRequest.STRATEGY -> builder.strategy(transformString(jsonValue, context));
                default -> throw new IllegalStateException("Unexpected value: " + key);
            }
        });

        return builder.build();
    }


}
