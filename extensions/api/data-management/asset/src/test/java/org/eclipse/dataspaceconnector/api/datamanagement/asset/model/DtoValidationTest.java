/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void verifyValidation_assetEntryDto_missingAsset() {
        var entry = AssetEntryDto.Builder.newInstance()
                .asset(null) //should break validation
                .dataAddress(DataAddressDto.Builder.newInstance().build())
                .build();
        var result = validator.validate(entry);
        assertThat(result).hasSize(1).allSatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("Asset cannot be null"));
    }

    @Test
    void verifyValidation_assetEntryDto_missingDataAddress() {
        var entry = AssetEntryDto.Builder.newInstance()
                .asset(AssetDto.Builder.newInstance().build())
                .dataAddress(null) // should break validation
                .build();
        var result = validator.validate(entry);
        assertThat(result).hasSize(1).allSatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("DataAddress cannot be null"));
    }

    @Test
    void verifyValidation_assetDto_missingProperties() {
        var asset = AssetDto.Builder.newInstance()
                .properties(null)
                .build();

        assertThat(validator.validate(asset)).allSatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("properties cannot be null"));
    }
}