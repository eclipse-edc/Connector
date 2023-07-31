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

package org.eclipse.edc.util.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    void getAllFieldsRecursive() {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");

        assertThat(ReflectionUtil.getAllFieldsRecursive(to.getClass())).hasSize(5).extracting(Field::getName)
                .containsExactlyInAnyOrder("description", "priority", "testProperty", "listField", "embedded");
    }

    @Test
    void getFieldRecursive_whenDeclaredInSuperclass() {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");
        assertThat(ReflectionUtil.getFieldRecursive(to.getClass(), "description")).isNotNull();
    }

    @Test
    void getFieldRecursive_whenDeclaredInClass() {
        var to = new TestObjectSubclass("test-desc", 1, "foobar");

        var testProperty = ReflectionUtil.getFieldRecursive(to.getClass(), "testProperty");

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
    void getFieldValue_whenParentExist() {
        var to = new TestObjectSubSubclass("test-desc", 1, "foobar");
        to.setAnotherObject(new AnotherObject("another-desc"));

        String fieldValue = ReflectionUtil.getFieldValue("anotherObject.anotherDescription", to);
        assertThat(fieldValue).isEqualTo("another-desc");
    }

    @Test
    void getFieldValue_whenParentNotExist() {
        var to = new TestObjectSubSubclass("test-desc", 1, "foobar");
        to.setAnotherObject(null);

        String fieldValue = ReflectionUtil.getFieldValue("anotherObject.anotherDescription", to);
        assertThat(fieldValue).isNull();
    }

    @Test
    void getFieldValue_withArrayIndex() {
        var to1 = new TestObject("to1", 420);
        var o = new TestObjectWithList("test-desc", 0, List.of(to1, new TestObject("to2", 69)));
        assertThat((TestObject) ReflectionUtil.getFieldValue("nestedObjects[0]", o)).isEqualTo(to1);
    }

    @Test
    void getFieldValue_arrayWithoutIndex() {
        var nestedObjects = List.of(
                new TestObject("to1", 420),
                new TestObject("to2", 69)
        );
        var object = new TestObjectWithList("test-desc", 0, nestedObjects);

        var result = ReflectionUtil.getFieldValue("nestedObjects.description", object);

        assertThat(result).isEqualTo(List.of("to1", "to2"));
    }

    @Test
    void getFieldValue_withArrayIndex_andDotAccess() {
        var to1 = new TestObject("to1", 420);
        var o = new TestObjectWithList("test-desc", 0, List.of(to1, new TestObject("to2", 69)));

        var fieldValue = ReflectionUtil.getFieldValue("nestedObjects[0].priority", o);

        assertThat(fieldValue).isEqualTo(420);
    }

    @Test
    void getFieldValue_withArrayIndex_outOfBounds() {
        var o = new TestObjectWithList("test-desc", 0, List.of(new TestObject("to1", 420), new TestObject("to2", 69)));
        assertThatThrownBy(() -> ReflectionUtil.getFieldValue("nestedObjects[3]", o)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void getSingleSuperTypeGenericArgument() {
        var fields = ReflectionUtil.getSingleSuperTypeGenericArgument(TestGenericSubclass.class, TestGenericObject.class);
        assertThat(fields).isEqualTo(String.class);
    }

    @Test
    void getSingleSuperTypeGenericArgument_whenNoGenericSuperclass() {
        var fields = ReflectionUtil.getSingleSuperTypeGenericArgument(TestObjectWithList.class, TestObject.class);
        assertThat(fields).isNull();
    }

    @Test
    void getSingleSuperTypeGenericArgument_whenGenericClass() {
        var genericList = new TestGenericArrayList<String>();
        var fields = ReflectionUtil.getSingleSuperTypeGenericArgument(genericList.getClass(), ArrayList.class);
        assertThat(fields).isNull();
    }
}
