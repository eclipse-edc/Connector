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

package org.eclipse.edc.query;

import org.eclipse.edc.spi.query.OperatorPredicate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContainsOperatorPredicateTest {

    private final OperatorPredicate predicate = new ContainsOperatorPredicate();

    @Test
    void shouldCheckIfRightOperandIsContainedInProperty() {
        assertThat(predicate.test(List.of("foo", "bar"), "bar")).isTrue();
        assertThat(predicate.test(List.of("foo", "bar"), "baz")).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTypeIsNotMatching() {
        assertThat(predicate.test(List.of("42", "bar"), 42)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenPropertyIsNotCollection() {
        assertThat(predicate.test("string", "string")).isFalse();
    }

}
