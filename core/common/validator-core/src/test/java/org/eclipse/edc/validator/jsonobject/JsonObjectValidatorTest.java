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

package org.eclipse.edc.validator.jsonobject;

import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class JsonObjectValidatorTest {

    @Test
    void shouldSucceed() {
        var input = createObjectBuilder();

        var result = JsonObjectValidator.newValidator().validate(input.build());

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldValidateRootLevelId() {
        var input = createObjectBuilder().add(ID, " ");

        var result = JsonObjectValidator.newValidator()
                .verify(ID, OptionalIdNotBlank::new)
                .validate(input.build());

        assertThat(result).isFailed();
    }

    @Test
    void shouldValidateMandatoryObject_success() {
        var input = createObjectBuilder().add("mandatoryField", createArrayBuilder().add(createObjectBuilder()));

        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryField", MandatoryField::new)
                .validate(input.build());

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldValidateMandatoryObject_failure() {
        var input = createObjectBuilder();

        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryField", MandatoryField::new)
                .validate(input.build());

        assertThat(result).isFailed();
    }
    
    @Test
    void shouldValidateNestedObject_whenMandatoryObject_success() {
        var input = createObjectBuilder()
                .add("mandatoryField", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("subField", createObjectBuilder()))
                );

        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryField", MandatoryField::new)
                .verifyObject("mandatoryField", v -> v
                        .verify("subField", MandatoryField::new))
                .validate(input.build());

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldValidateNestedObject_whenMandatoryObject_failure() {
        var input = createObjectBuilder()
                .add("mandatoryField", createArrayBuilder()
                        .add(createObjectBuilder())
                );

        var result = JsonObjectValidator.newValidator()
                .verify("mandatoryField", MandatoryField::new)
                .verifyObject("mandatoryField", v -> v
                        .verify("subField", MandatoryField::new))
                .validate(input.build());

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).anySatisfy(violation -> {
                assertThat(violation.path()).contains("mandatoryField/subField");
            });
        });
    }

    @Test
    void shouldValidateNestedObject_succeed_whenNestedObjectIsMissing() {
        var input = createObjectBuilder();

        var result = JsonObjectValidator.newValidator()
                .verifyObject("optionalField", v -> v
                        .verify("subField", MandatoryField::new))
                .validate(input.build());

        assertThat(result).isSucceeded();
    }

}
