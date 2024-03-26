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

class LikeOperatorPredicateTest {

    private final OperatorPredicate predicate = new LikeOperatorPredicate();

    @Test
    void shouldHandlePercentAtTheStartOfTheString() {
        assertThat(predicate.test("this is a test", "%test")).isTrue();
        assertThat(predicate.test("not tested", "%test")).isFalse();
    }

    @Test
    void shouldHandlePercentAtTheEndOfTheString() {
        assertThat(predicate.test("test valid", "test%")).isTrue();
        assertThat(predicate.test(" test invalid", "test%")).isFalse();
    }

    @Test
    void shouldHandlePercentAtTheStartAndEndOfTheString() {
        assertThat(predicate.test("this test valid", "%test%")).isTrue();
        assertThat(predicate.test("test also valid", "%test%")).isTrue();
        assertThat(predicate.test("valid is the test", "%test%")).isTrue();
        assertThat(predicate.test("invalid", "%test%")).isFalse();
    }

}
