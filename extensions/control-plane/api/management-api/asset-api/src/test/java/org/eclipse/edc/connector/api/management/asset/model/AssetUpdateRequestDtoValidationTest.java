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

package org.eclipse.edc.connector.api.management.asset.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetUpdateRequestDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void verifyValidation_assetDto_missingProperties() {
        var asset = AssetUpdateRequestDto.Builder.newInstance()
                .properties(null)
                .build();

        var result = validator.validate(asset);

        assertThat(result).anySatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("properties cannot be null"));
    }

    @Test
    void verifyValidation_assetDto_duplicateProperties() {
        var asset = AssetCreationRequestDto.Builder.newInstance()
                .properties(Map.of("key", "value"))
                .privateProperties(Map.of("key", "value"))
                .build();

        var result = validator.validate(asset);

        assertThat(result).anySatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("no duplicate keys in properties and private properties"));
    }

}
