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

package org.eclipse.edc.validator.jsonobject;

import org.eclipse.edc.validator.jsonobject.validators.MissingPrefixes;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class MissingPrefixesTest {


    private final JsonObjectValidator validator = JsonObjectValidator.newValidator()
            .verify(path -> new MissingPrefixes(path, () -> Set.of("prefix"))).build();

    @Test
    void shouldValidateObjectMissingPrefixes_success() {
        var input = createObjectBuilder()
                .add("mandatoryObject", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("@id", "id")
                                .add("@type", createArrayBuilder().add("type"))
                                .add("subProperty", ""))
                );

        var result = validator.validate(input.build());

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldValidateObjectMissingPrefixes_failure() {
        var input = createObjectBuilder()
                .add("prefix:mandatoryObject", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("subProperty", ""))
                );

        var result = validator.validate(input.build());

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).anySatisfy(violation -> {
                assertThat(violation.path()).contains("prefix:mandatoryObject");
            });
        });
    }

    @Test
    void shouldValidateMissingPrefixes_whenNestedProperty_failure() {
        var input = createObjectBuilder()
                .add("mandatoryObject", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("prefix:subProperty", ""))
                );

        var result = validator.validate(input.build());

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).anySatisfy(violation -> {
                assertThat(violation.path()).contains("prefix:subProperty");
            });
        });
    }


    @Test
    void shouldValidateNestedMissingPrefixes_whenPrefixedId_failure() {
        var input = createObjectBuilder()
                .add("mandatoryObject", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("@id", "prefix:subProperty"))
                );

        var result = validator.validate(input.build());

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).anySatisfy(violation -> {
                assertThat(violation.path()).contains("mandatoryObject");
                assertThat(violation.message()).contains("Value of @id contains a prefix");
            });
        });
    }

    @Test
    void shouldValidateNestedMissingPrefixes_whenPrefixedType_failure() {
        var input = createObjectBuilder()
                .add("mandatoryObject", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("@type", createArrayBuilder().add("prefix:subProperty")))
                );

        var result = validator.validate(input.build());

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).anySatisfy(violation -> {
                assertThat(violation.path()).contains("mandatoryObject");
                assertThat(violation.message()).contains("Value of @type contains a prefix");
            });
        });
    }
}
