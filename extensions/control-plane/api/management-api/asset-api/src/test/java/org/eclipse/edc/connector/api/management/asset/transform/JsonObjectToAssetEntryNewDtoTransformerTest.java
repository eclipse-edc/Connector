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

package org.eclipse.edc.connector.api.management.asset.transform;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_ASSET;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_DATA_ADDRESS;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectToAssetEntryNewDtoTransformerTest {

    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private JsonObjectToAssetEntryNewDtoTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToAssetEntryNewDtoTransformer();
    }

    @Test
    void transform() {
        var jsonObject = Json.createObjectBuilder()
                .add(TYPE, EDC_ASSET_ENTRY_DTO_TYPE)
                .add(EDC_ASSET_ENTRY_DTO_ASSET, Json.createObjectBuilder().build())
                .add(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, Json.createObjectBuilder().build())
                .build();

        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(Asset.class))).thenReturn(Asset.Builder.newInstance().build());
        when(context.transform(any(JsonValue.class), eq(DataAddress.class))).thenReturn(DataAddress.Builder.newInstance().type("test").build());


        var dto = transformer.transform(jsonLd.expand(jsonObject).getContent(), context);

        assertThat(dto).isNotNull();
        assertThat(dto.getAsset()).isNotNull();
        assertThat(dto.getDataAddress()).isNotNull();
    }
}
