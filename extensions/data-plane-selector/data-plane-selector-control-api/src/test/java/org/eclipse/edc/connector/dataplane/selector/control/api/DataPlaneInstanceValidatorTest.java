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

package org.eclipse.edc.connector.dataplane.selector.control.api;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_SOURCE_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.ALLOWED_TRANSFER_TYPES;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.URL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class DataPlaneInstanceValidatorTest {

    private final Validator<JsonObject> validator = DataPlaneInstanceValidator.instance();

    @Test
    void shouldPass_whenJsonIsValid() {
        var json = createObjectBuilder()
                .add(ID, "id")
                .add(URL, value("url"))
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder().add(value("src")))
                .add(ALLOWED_TRANSFER_TYPES, createArrayBuilder().add(value("transfer")))
                .build();

        var result = validator.validate(json);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryFieldsAreNotSet() {
        var json = createObjectBuilder().build();

        var result = validator.validate(json);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations, InstanceOfAssertFactories.list(Violation.class))
                .isNotEmpty()
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(ID))
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(URL))
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(ALLOWED_SOURCE_TYPES))
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(ALLOWED_TRANSFER_TYPES));
    }

    @Test
    void shouldFail_whenAllowedTypesAreEmpty() {
        var json = createObjectBuilder()
                .add(ALLOWED_SOURCE_TYPES, createArrayBuilder())
                .add(ALLOWED_TRANSFER_TYPES, createArrayBuilder())
                .build();

        var result = validator.validate(json);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations, InstanceOfAssertFactories.list(Violation.class))
                .isNotEmpty()
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(ALLOWED_SOURCE_TYPES))
                .anySatisfy(v -> assertThat(v.path()).isEqualTo(ALLOWED_TRANSFER_TYPES));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
