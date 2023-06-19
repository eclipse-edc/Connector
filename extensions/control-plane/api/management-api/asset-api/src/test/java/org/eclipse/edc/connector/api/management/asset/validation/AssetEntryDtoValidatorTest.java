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

package org.eclipse.edc.connector.api.management.asset.validation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_ASSET;
import static org.eclipse.edc.connector.api.management.asset.model.AssetEntryNewDto.EDC_ASSET_ENTRY_DTO_DATA_ADDRESS;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_PRIVATE_PROPERTIES;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_PROPERTIES;

class AssetEntryDtoValidatorTest {

    private final Validator<JsonObject> validator = AssetEntryDtoValidator.assetEntryValidator();

    @Test
    void shouldSucceed_whenValidInput() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_ENTRY_DTO_ASSET, validAsset())
                .add(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenNoAssetAndNoDataAddress() {
        var input = createObjectBuilder()
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(2);
        });
    }

    @Test
    void shouldFail_whenAssetIdIsBlank() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_ENTRY_DTO_ASSET, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(ID, " ")
                                .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder()))
                        ))
                .add(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> {
                assertThat(v.path()).isEqualTo(EDC_ASSET_ENTRY_DTO_ASSET + "/" + "@id");
            });
        });
    }

    @Test
    void shouldFail_whenAssetPropertiesAreMissing() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_ENTRY_DTO_ASSET, createArrayBuilder().add(createObjectBuilder()))
                .add(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v ->
                    assertThat(v.path()).isEqualTo(EDC_ASSET_ENTRY_DTO_ASSET + "/" + EDC_ASSET_PROPERTIES));
        });
    }

    @Test
    void shouldFail_whenAssetPropertiesAndPrivatePropertiesHaveDuplicatedKeys() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_ENTRY_DTO_ASSET, createArrayBuilder().add(createObjectBuilder()
                        .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add("key", createArrayBuilder())))
                        .add(EDC_ASSET_PRIVATE_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add("key", createArrayBuilder())))
                ))
                .add(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v ->
                    assertThat(v.path()).isEqualTo(EDC_ASSET_ENTRY_DTO_ASSET + "/" + EDC_ASSET_PROPERTIES));
        });
    }

    @Test
    void shouldFail_whenDataAddressHasNoType() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_ENTRY_DTO_ASSET, validAsset())
                .add(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS, createArrayBuilder().add(createObjectBuilder()))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v ->
                    assertThat(v.path()).isEqualTo(EDC_ASSET_ENTRY_DTO_DATA_ADDRESS + "/" + EDC_DATA_ADDRESS_TYPE_PROPERTY));
        });
    }

    private JsonArrayBuilder validDataAddress() {
        return createArrayBuilder().add(createObjectBuilder()
                .add(EDC_DATA_ADDRESS_TYPE_PROPERTY, createArrayBuilder().add(createObjectBuilder().add(VALUE, "AddressType")))
        );
    }

    private JsonArrayBuilder validAsset() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder()))
                );
    }
}
