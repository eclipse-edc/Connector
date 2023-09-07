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

package org.eclipse.edc.connector.dataplane.selector.api.v2.validation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_DEST_TYPES;
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
                .add(ALLOWED_DEST_TYPES, createArrayBuilder().add("test"))
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
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            Assertions.assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> {
                Assertions.assertThat(v.path()).isEqualTo(ID);
            });
        });
    }

    @Test
    void shouldFail_whenUrlIsMissing() {
        var input = createObjectBuilder()
                .add(ID, "my_id")
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            Assertions.assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> {
                Assertions.assertThat(v.path()).isEqualTo(URL);
            });
        });
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
