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

import org.junit.jupiter.api.Test;

import java.util.Comparator;
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
}