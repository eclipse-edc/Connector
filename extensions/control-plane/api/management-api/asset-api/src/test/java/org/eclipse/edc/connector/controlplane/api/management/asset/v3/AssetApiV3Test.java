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

package org.eclipse.edc.connector.controlplane.api.management.asset.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.edc.connector.controlplane.api.management.asset.v3.AssetApiV3.AssetInputSchema.ASSET_INPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.api.management.asset.v3.AssetApiV3.AssetOutputSchema.ASSET_OUTPUT_EXAMPLE;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_DATA_ADDRESS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.test.TestJsonLd.expand;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetApiV3Test {
    private final TypeManager typeManager = mock();
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToAssetTransformer());
        transformer.register(new JsonValueToGenericTypeTransformer(typeManager, "test"));
        transformer.register(new JsonObjectToDataAddressTransformer());
        when(typeManager.getMapper("test")).thenReturn(objectMapper);
    }

    @Test
    void assetInputExample() throws JsonProcessingException {
        var validator = AssetValidator.instance();

        var jsonObject = objectMapper.readValue(ASSET_INPUT_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = expand(jsonObject);
        assertThat(validator.validate(expanded)).isSucceeded();
        var transformed = transformer.transform(expanded, Asset.class).getContent();
        assertThat(transformed).isNotNull()
                .satisfies(t -> {
                    assertThat(t.getId()).isNotBlank();
                    assertThat(t.getProperties()).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
                    assertThat(t.getPrivateProperties()).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
                    assertThat(t.getDataAddress().getProperties()).asInstanceOf(map(String.class, Object.class)).isNotEmpty();
                });
    }

    @Test
    void assetOutputExample() throws JsonProcessingException {
        var jsonObject = objectMapper.readValue(ASSET_OUTPUT_EXAMPLE, JsonObject.class);
        var expanded = expand(jsonObject);

        assertThat(expanded.getString(ID)).isNotBlank();
        assertThat(expanded.getJsonArray(EDC_ASSET_PROPERTIES)).asList().isNotEmpty();
        assertThat(expanded.getJsonArray(EDC_ASSET_PRIVATE_PROPERTIES)).asList().isNotEmpty();
        assertThat(expanded.getJsonArray(EDC_ASSET_DATA_ADDRESS)).asList().isNotEmpty();
    }

}
