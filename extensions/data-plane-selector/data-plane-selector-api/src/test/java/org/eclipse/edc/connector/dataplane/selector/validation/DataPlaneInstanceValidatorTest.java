/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.validation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.api.validation.DataPlaneInstanceValidator;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class DataPlaneInstanceValidatorTest {
    private final Validator<JsonObject> validator = DataPlaneInstanceValidator.instance();

    @Test
    void shouldSucceed_whenValidInput() {
        var input = createObjectBuilder()
                .add(URL, value("http://myurl"))
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder().add("test"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenIdIsBlank() {
        var input = createObjectBuilder()
                .add(ID, " ")
                .add(URL, value("http://myurl"))
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder().add("test"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> assertThat(v.path()).isEqualTo(ID)));
    }

    @Test
    void shouldFail_whenUrlIsMissing() {
        var input = createObjectBuilder()
                .add(ID, "my_id")
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder().add("test"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> assertThat(v.path()).isEqualTo(URL)));
    }

    @Test
    void shouldFail_whenAllowedSourceEmpty() {
        var input = createObjectBuilder()
                .add(ID, "my_id")
                .add(URL, value("http://myurl"))
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> assertThat(failure.getViolations()).hasSize(1));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
