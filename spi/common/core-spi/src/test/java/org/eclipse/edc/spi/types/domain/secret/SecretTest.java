/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.spi.types.domain.secret;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SecretTest {

    private TypeManager typeManager;

    @BeforeEach
    void setUp() {
        typeManager = new JacksonTypeManager();
    }

    @Test
    void verifySerDeser() {
        var secret = Secret.Builder.newInstance().id("abcd123")
                .value("valuetest")
                .build();

        var serialized = typeManager.writeValueAsString(secret);
        var deserialized = typeManager.readValue(serialized, Secret.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(secret);
    }

    @Test
    void verifyExceptionWhenValueMissing() {
        assertThatNullPointerException().isThrownBy(() -> Secret.Builder.newInstance().id("abcd123").build())
                .withMessage("`value` is missing");
    }
}