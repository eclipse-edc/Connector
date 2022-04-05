/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReflectionUtilTest {

    @Test
    void getFieldValue() {
        var value = ReflectionUtil.getFieldValue("description", new TestObject("test-desc", 1));
        assertThat(value).isInstanceOf(String.class).isEqualTo("test-desc");

        var value2 = ReflectionUtil.getFieldValue("priority", new TestObject("test-desc", 1));
        assertThat(value2).isInstanceOf(Integer.class).isEqualTo(1);
    }

    @Test
    void getFieldValue_isNull() {
        var value = ReflectionUtil.getFieldValue("description", new TestObject(null, 1));
        assertThat(value).isNull();
    }

    @Test
    void getFieldValue_notExist() {
        assertThatThrownBy(() -> ReflectionUtil.getFieldValue("notExist", new TestObject("test-desc", 1)))
                .isInstanceOf(ReflectionException.class);
    }

    @Test
    void getFieldValue_invalidArgs() {

        assertThatThrownBy(() -> ReflectionUtil.getFieldValue("", new TestObject("test-desc", 1)))
                .isInstanceOf(ReflectionException.class);

        assertThatThrownBy(() -> ReflectionUtil.getFieldValue(null, new TestObject("test-desc", 1)))
                .isInstanceOf(NullPointerException.class).hasMessage("propertyName");

        assertThatThrownBy(() -> ReflectionUtil.getFieldValue("description", null))
                .isInstanceOf(NullPointerException.class).hasMessage("object");
    }

    @Test
    void getFieldValue_fromMap() {
        var value = ReflectionUtil.getFieldValue("key", Map.of("key", "value"));

        assertThat(value).isEqualTo("value");
    }

    @Test
    void getFieldValueSilent() {
        var value = ReflectionUtil.getFieldValueSilent("description", new TestObject("test-desc", 1));
        assertThat(value).isInstanceOf(String.class).isEqualTo("test-desc");
    }

    @Test
    void getFieldValueSilent_isNull() {
        var value = ReflectionUtil.getFieldValueSilent("description", new TestObject(null, 1));
        assertThat(value).isNull();
    }

    @Test
    void getFieldValueSilent_notExist() {
        var value = ReflectionUtil.getFieldValueSilent("notExist", new TestObject(null, 1));
        assertThat(value).isNull();
    }

    @Test
    void getFieldValueSilent_invalidArgs() {
        // technically already handled in the "_notExist" test
        var fieldValueSilent = ReflectionUtil.getFieldValueSilent("", new TestObject("test-desc", 1));
        assertThat(fieldValueSilent).isNull();

        assertThatThrownBy(() -> ReflectionUtil.getFieldValueSilent(null, new TestObject("test-desc", 1)))
                .isInstanceOf(NullPointerException.class).hasMessage("propertyName");

        assertThatThrownBy(() -> ReflectionUtil.getFieldValueSilent("description", null))
                .isInstanceOf(NullPointerException.class).hasMessage("object");
    }

    @Test
    void propertyComparator_whenAscending() {
        var testObjects = IntStream.range(0, 10).mapToObj(i -> new TestObject("id" + i, i)).collect(Collectors.toList());
        var comparator = ReflectionUtil.propertyComparator(true, "description");
        assertThat(testObjects.stream().sorted(comparator)).isSortedAccordingTo(Comparator.comparing(TestObject::getDescription));
    }

    @Test
    void propertyComparator_whenDescending() {
        var testObjects = IntStream.range(0, 10).mapToObj(i -> new TestObject("id" + i, i)).collect(Collectors.toList());
        var comparator = ReflectionUtil.propertyComparator(false, "description");
        assertThat(testObjects.stream().sorted(comparator)).isSortedAccordingTo(Comparator.comparing(TestObject::getDescription).reversed());
    }

    @Test
    void propertyComparator_whenPropertyNotFound() {
        var testObjects = IntStream.range(0, 10).mapToObj(i -> new TestObject("id" + i, i)).collect(Collectors.toList());
        var comparator = ReflectionUtil.propertyComparator(true, "notexist");
        assertThat(testObjects.stream().sorted(comparator)).containsExactlyInAnyOrder(testObjects.toArray(new TestObject[]{}));
    }

    @Test
    void getAllFieldsRecursive() {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");

        assertThat(ReflectionUtil.getAllFieldsRecursive(to.getClass())).hasSize(3).extracting(Field::getName)
                .containsExactlyInAnyOrder("description", "priority", "testProperty");
    }

    @Test
    void getFieldRecursive_whenDeclaredInSuperclass() {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");
        assertThat(ReflectionUtil.getFieldRecursive(to.getClass(), "description")).isNotNull();
    }

    @Test
    void getFieldRecursive_whenDeclaredInClass() throws IllegalAccessException {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");
        Field testProperty = ReflectionUtil.getFieldRecursive(to.getClass(), "testProperty");
        assertThat(testProperty).isNotNull();
    }

    @Test
    @DisplayName("Should pick the property from the object highest in the object hierarchy")
    void getFieldRecursive_whenDeclaredInBoth() {
        var to = new TestObjectSubSubclass("test-desc", 2, "foobar");
        assertThat(ReflectionUtil.getFieldRecursive(to.getClass(), "description")).isNotNull().extracting(Field::getDeclaringClass)
                .isEqualTo(TestObject.class);

    }

    @Test
    void getFieldRecursive_whenNotDeclared() {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");
        assertThat(ReflectionUtil.getFieldRecursive(to.getClass(), "notExist")).isNull();
    }

    @Test
    void getFieldValue_withArrayIndex() {
        var to1 = new TestObject("to1", 420);
        var o = new TestObjectWithList("test-desc", 0, List.of(to1, new TestObject("to2", 69)));
        assertThat((TestObject) ReflectionUtil.getFieldValue("nestedObjects[0]", o)).isEqualTo(to1);
    }

    @Test
    void getFieldValue_withArrayIndex_andDotAccess() {
        var to1 = new TestObject("to1", 420);
        var o = new TestObjectWithList("test-desc", 0, List.of(to1, new TestObject("to2", 69)));
        assertThat((int) ReflectionUtil.getFieldValue("nestedObjects[0].priority", o)).isEqualTo(420);
    }

    @Test
    void getFieldValue_withArrayIndex_outOfBounds() {
        var o = new TestObjectWithList("test-desc", 0, List.of(new TestObject("to1", 420), new TestObject("to2", 69)));
        assertThatThrownBy(() -> ReflectionUtil.getFieldValue("nestedObjects[3]", o)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}