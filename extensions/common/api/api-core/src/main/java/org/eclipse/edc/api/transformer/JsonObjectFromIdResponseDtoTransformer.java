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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.api.model.IdResponseDto.EDC_ID_RESPONSE_DTO_CREATED_AT;
import static org.eclipse.edc.api.model.IdResponseDto.EDC_ID_RESPONSE_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromIdResponseDtoTransformer extends AbstractJsonLdTransformer<IdResponseDto, JsonObject> {

    private final JsonBuilderFactory builderFactory;

    public JsonObjectFromIdResponseDtoTransformer(JsonBuilderFactory builderFactory) {
        super(IdResponseDto.class, JsonObject.class);
        this.builderFactory = builderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull IdResponseDto input, @NotNull TransformerContext context) {
        return builderFactory.createObjectBuilder()
                .add(TYPE, EDC_ID_RESPONSE_DTO_TYPE)
                .add(ID, input.getId())
                .add(EDC_ID_RESPONSE_DTO_CREATED_AT, input.getCreatedAt())
                .build();
    }
}
