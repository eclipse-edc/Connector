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

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_ASSET_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_CONNECTOR_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_CONTRACT_ID;
import static org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto.EDC_DATA_REQUEST_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromDataRequestDtoTransformer extends AbstractJsonLdTransformer<DataRequestDto, JsonObject> {

    private final JsonBuilderFactory builderFactory;

    public JsonObjectFromDataRequestDtoTransformer(JsonBuilderFactory builderFactory) {
        super(DataRequestDto.class, JsonObject.class);
        this.builderFactory = builderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataRequestDto input, @NotNull TransformerContext context) {
        var builder = builderFactory.createObjectBuilder()
                .add(TYPE, EDC_DATA_REQUEST_DTO_TYPE)
                .add(ID, input.getId());

        Optional.ofNullable(input.getAssetId()).ifPresent(it -> builder.add(EDC_DATA_REQUEST_DTO_ASSET_ID, it));
        Optional.ofNullable(input.getContractId()).ifPresent(it -> builder.add(EDC_DATA_REQUEST_DTO_CONTRACT_ID, it));
        Optional.ofNullable(input.getConnectorId()).ifPresent(it -> builder.add(EDC_DATA_REQUEST_DTO_CONNECTOR_ID, it));

        return builder.build();
    }
}
