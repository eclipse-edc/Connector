/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.util.collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionUtilTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void isEmpty() {
        assertThat(CollectionUtil.isEmpty(null)).isTrue();
        assertThat(CollectionUtil.isEmpty(Collections.emptyList())).isTrue();
        assertThat(CollectionUtil.isEmpty(Collections.singletonList("test"))).isFalse();
        assertThat(CollectionUtil.isEmpty(Arrays.asList("foo", "bar", "baz"))).isFalse();
    }

    @Test
    void isNotEmpty() {
        assertThat(CollectionUtil.isNotEmpty((Collection<?>) null)).isFalse();
        assertThat(CollectionUtil.isNotEmpty(Collections.emptyList())).isFalse();
        assertThat(CollectionUtil.isNotEmpty(Collections.singletonList("test"))).isTrue();
        assertThat(CollectionUtil.isNotEmpty(Arrays.asList("foo", "bar", "baz"))).isTrue();
    }

    @Test
    void isNotEmpty_map() {
        assertThat(CollectionUtil.isNotEmpty((Map<?, ?>) null)).isFalse();
        assertThat(CollectionUtil.isNotEmpty(Collections.emptyMap())).isFalse();
        assertThat(CollectionUtil.isNotEmpty(Collections.singletonMap("testkey", "testValue"))).isTrue();
        assertThat(CollectionUtil.isNotEmpty(Map.of("foo", "bar", "foo2", "baz"))).isTrue();
    }

    @Test
    void isAnyOf() {
        int[] nullArray = null;
        assertThat(CollectionUtil.isAnyOf(15, nullArray)).isFalse();
        assertThat(CollectionUtil.isAnyOf(15, 15, 16, 17)).isTrue();
        assertThat(CollectionUtil.isAnyOf("foobar", "foo", "bar", "baz", "foobar")).isTrue();
        assertThat(CollectionUtil.isAnyOf("foobar", "bar", "baz")).isFalse();
    }
}
