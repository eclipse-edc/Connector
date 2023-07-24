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

package org.eclipse.edc.connector.defaults.storage;

import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.defaults.storage.CriterionToPredicateConverterImplTest.TestEnum.ENTRY1;
import static org.eclipse.edc.connector.defaults.storage.CriterionToPredicateConverterImplTest.TestEnum.ENTRY2;

class CriterionToPredicateConverterImplTest {

    private final CriterionToPredicateConverterImpl converter = new CriterionToPredicateConverterImpl();

    @Test
    void equal() {
        var predicate = converter.convert(new Criterion("value", "=", "any"));

        assertThat(predicate)
                .accepts(new TestObject("any"))
                .rejects(new TestObject("other"), new TestObject(""), new TestObject((String) null));
    }

    @Test
    void equal_enumShouldCheckEntryName() {
        var predicate = converter.convert(new Criterion("enumValue", "=", "ENTRY2"));

        assertThat(predicate)
                .accepts(new TestObject("any", ENTRY2))
                .rejects(new TestObject("any", ENTRY1), new TestObject("any", null));
    }

    @Test
    void equal_integerAndDouble() {
        var predicate = converter.convert(new Criterion("intValue", "=", 42));

        assertThat(predicate)
                .rejects(new TestObject("any", ENTRY2), new TestObject(-1))
                .accepts(new TestObject(42));
    }

    @Test
    void equal_shouldMatchPredicate_whenObjectIsList() {
        var predicate = converter.convert(new Criterion("list.value", "=", "two"));

        assertThat(predicate)
                .accepts(new TestObject(List.of(new NestedObject("one"), new NestedObject("two"))))
                .rejects(new TestObject(List.of(new NestedObject("three"))));
    }

    @Test
    void equal_shouldNotMatch_whenPropertyDoesNotExist() {
        var predicate = converter.convert(new Criterion("not-existent", "=", 42));

        assertThat(predicate).rejects(new TestObject(42));
    }

    @Test
    void in() {
        var predicate = converter.convert(new Criterion("value", "in", List.of("first", "second")));

        assertThat(predicate)
                .accepts(new TestObject("first"), new TestObject("second"))
                .rejects(new TestObject("third"), new TestObject(""), new TestObject((String) null));
    }

    @Test
    void in_throwsException() {
        var predicate = converter.convert(new Criterion("value", "in", "(first, second)"));
        assertThatThrownBy(() -> predicate.test(new TestObject("first")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operator IN requires the right-hand operand to be an");
    }

    @Test
    void in_shouldThrowException_whenPropertyDoesNotExist() {
        var predicate = converter.convert(new Criterion("not-existent", "in", List.of("any")));

        assertThat(predicate).rejects(new TestObject("any"));
    }

    @Test
    void like_shouldHandlePercentAtTheStartOfTheString() {
        var predicate = converter.convert(new Criterion("value", "like", "%test"));

        assertThat(predicate)
                .accepts(new TestObject("this is a test"))
                .rejects(new TestObject("not tested"));
    }

    @Test
    void like_shouldHandlePercentAtTheEndOfTheString() {
        var predicate = converter.convert(new Criterion("value", "like", "test%"));

        assertThat(predicate)
                .accepts(new TestObject("test valid"))
                .rejects(new TestObject(" test invalid"));
    }

    @Test
    void like_shouldHandlePercentAtTheStartAndEndOfTheString() {
        var predicate = converter.convert(new Criterion("value", "like", "%test%"));

        assertThat(predicate)
                .accepts(new TestObject("this test valid"), new TestObject("test also valid"), new TestObject("valid is the test"))
                .rejects(new TestObject("invalid"));
    }

    @Test
    void like_shouldThrowException_whenPropertyDoesNotExits() {
        var predicate = converter.convert(new Criterion("not-existent", "like", "any"));

        assertThat(predicate).rejects(new TestObject(List.of(new NestedObject("any"))));
    }

    public enum TestEnum {
        ENTRY1, ENTRY2
    }

    private static class TestObject {
        private String value;
        private TestEnum enumValue;
        private Integer intValue;
        private List<NestedObject> list;

        private TestObject(String value) {
            this(value, null);
        }

        private TestObject(int value) {
            this.intValue = value;
        }

        private TestObject(List<NestedObject> list) {
            this.list = list;
        }

        private TestObject(String value, TestEnum enumValue) {
            this.value = value;
            this.enumValue = enumValue;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "value='" + value + '\'' +
                    ", enumValue=" + enumValue +
                    ", intValue=" + intValue +
                    ", list=" + list +
                    '}';
        }
    }

    private record NestedObject(String value) { }

}
