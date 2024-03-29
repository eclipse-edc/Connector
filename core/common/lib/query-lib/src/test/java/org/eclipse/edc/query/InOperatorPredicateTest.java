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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InOperatorPredicateTest {

    private final OperatorPredicate predicate = new InOperatorPredicate();

    @Test
    void shouldReturnTrue_whenPropertyValueIsContainedInTheList() {
        assertThat(predicate.test("first", List.of("first", "second"))).isTrue();
        assertThat(predicate.test("third", List.of("first", "second"))).isFalse();
        assertThat(predicate.test("", List.of("first", "second"))).isFalse();
    }

    @Test
    void shouldThrowException_whenOperandRightIsNotList() {
        assertThatThrownBy(() -> predicate.test("any", "(first, second)"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operator IN requires the right-hand operand to be an");
    }

}
