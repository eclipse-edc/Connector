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

package org.eclipse.edc.api.validation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;

class DataAddressValidatorTest {

    private final Validator<JsonObject> validator = DataAddressValidator.instance();

    @Test
    void shouldSucceed_whenObjectIsValid() {
        var contractDefinition = createObjectBuilder()
                .add(EDC_DATA_ADDRESS_TYPE_PROPERTY, value("value"))
                .build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryFieldsAreMissing() {
        var contractDefinition = createObjectBuilder().build();

        var result = validator.validate(contractDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDC_DATA_ADDRESS_TYPE_PROPERTY));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
