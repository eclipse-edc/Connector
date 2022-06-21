/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.query;

import org.eclipse.dataspaceconnector.common.reflection.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseCriterionToPredicateConverterTest {

    private final TestCriterionToPredicateConverter converter = new TestCriterionToPredicateConverter();

    @Test
    void convertEqual() {
        var predicate = converter.convert(new Criterion("value", "=", "any"));

        assertThat(predicate)
                .accepts(new TestObject("any"))
                .rejects(new TestObject("other"), new TestObject(""), new TestObject(null));
    }

    @Test
    void convertIn_throwsException() {
        var predicate = converter.convert(new Criterion("value", "in", "(first, second)"));
        assertThatThrownBy(() -> predicate.test(new TestObject("first")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operator IN requires the right-hand operand to be an");

    }

    @Test
    void convertIn() {
        var predicate = converter.convert(new Criterion("value", "in", List.of("first", "second")));

        assertThat(predicate)
                .accepts(new TestObject("first"), new TestObject("second"))
                .rejects(new TestObject("third"), new TestObject(""), new TestObject(null));
    }

    private static class TestCriterionToPredicateConverter extends BaseCriterionToPredicateConverter<TestObject> {

        @Override
        protected <R> R property(String key, Object object) {
            return ReflectionUtil.getFieldValueSilent(key, object);
        }
    }

    private static class TestObject {
        private final String value;

        private TestObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }
}