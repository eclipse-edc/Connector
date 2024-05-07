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
import static org.eclipse.edc.query.NotEqualOperatorPredicateTest.TestEnum.ENTRY1;
import static org.eclipse.edc.query.NotEqualOperatorPredicateTest.TestEnum.ENTRY2;

class NotEqualOperatorPredicateTest {
    private final OperatorPredicate predicate = new NotEqualOperatorPredicate();

    @Test
    void shouldReturnTrue_whenObjectsAreEqual() {
        assertThat(predicate.test("any", "any")).isFalse();
        assertThat(predicate.test("other", "any")).isTrue();
        assertThat(predicate.test("", "any")).isTrue();
        assertThat(predicate.test(null, "any")).isTrue();
        assertThat(predicate.test(null, null)).isFalse();
        assertThat(predicate.test(42, 42)).isFalse();
        assertThat(predicate.test(42, 69)).isTrue();
    }

    @Test
    void shouldCheckName_whenPropertyIsEnum() {
        assertThat(predicate.test(ENTRY2, "ENTRY2")).isFalse();
        assertThat(predicate.test(ENTRY1, "ENTRY2")).isTrue();
    }

    @Test
    void shouldCheckOrdinal_whenPropertyIsEnumAndOperandRightIsNumber() {
        assertThat(predicate.test(ENTRY2, ENTRY2.ordinal())).isFalse();
        assertThat(predicate.test(ENTRY1, ENTRY2.ordinal())).isTrue();
        assertThat(predicate.test(ENTRY2, -42)).isTrue();
    }

    @Test
    void shouldMatchPredicate_whenObjectIsList() {
        assertThat(predicate.test(List.of("one", "two"), "two")).isFalse();
        assertThat(predicate.test(List.of("one", "two"), "three")).isTrue();
    }

    public enum TestEnum {
        ENTRY1, ENTRY2
    }
}