/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyDefinitionRequestDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void verifyValidation_policyDefinitionDto_missingId() {
        PolicyDefinition policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        var policy = PolicyDefinitionRequestDto.Builder.newInstance()
                .policy(policyDefinition.getPolicy())
                .id(null)
                .build();

        var result = validator.validate(policy);
        assertThat(result).anySatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("must not be null"));
    }

    @Test
    void verifyValidation_policyDefinitionDto_missingPolicy() {
        var policy = PolicyDefinitionRequestDto.Builder.newInstance()
                .id("id")
                .policy(null)
                .build();

        var result = validator.validate(policy);
        assertThat(result).anySatisfy(cv -> assertThat(cv.getMessage()).isEqualTo("must not be null"));
    }

}