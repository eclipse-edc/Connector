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

import static org.assertj.core.api.Assertions.assertThat;

class IlikeOperatorPredicateTest {

    private final OperatorPredicate predicate = new IlikeOperatorPredicate();

    @Test
    void shouldHandlePercentAtTheStartOfTheString() {
        assertThat(predicate.test("THIS IS A TEST", "%test")).isTrue();
        assertThat(predicate.test("NOT TESTED", "%test")).isFalse();
    }

    @Test
    void shouldHandlePercentAtTheEndOfTheString() {
        assertThat(predicate.test("TEST VALID", "test%")).isTrue();
        assertThat(predicate.test(" TEST INVALID", "test%")).isFalse();
    }

    @Test
    void shouldHandlePercentAtTheStartAndEndOfTheString() {
        assertThat(predicate.test("THIS TEST VALID", "%test%")).isTrue();
        assertThat(predicate.test("TEST ALSO VALID", "%test%")).isTrue();
        assertThat(predicate.test("VALID IS THE TEST", "%test%")).isTrue();
        assertThat(predicate.test("INVALID", "%test%")).isFalse();
    }

}
