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

package org.eclipse.edc.connector.api.management.secret.validation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;

class SecretsValidatorTest {

    private final Validator<JsonObject> validator = SecretsValidator.instance();

    @Test
    void validate_whenValidInput_thenSucceed() {
        // ID is automatically added if missing
        var input = createObjectBuilder()
                .add(ID, "secret-id")
                .add(EDC_SECRET_VALUE, value("secret-value"))
                .build();


        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenIdIsBlank_thenFail() {
        var input = createObjectBuilder()
                .add(ID, " ")
                .add(EDC_SECRET_VALUE, value("secret-value"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> {
            assertThat(v.path()).isEqualTo(ID);
        }));
    }


    @Test
    void validate_whenSecretValueIsMissing_thenFail() {
        var input = createObjectBuilder()
                .add(ID, "secret-id")
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations()).hasSize(1).anySatisfy(v ->
                assertThat(v.path()).isEqualTo(EDC_SECRET_VALUE)));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
