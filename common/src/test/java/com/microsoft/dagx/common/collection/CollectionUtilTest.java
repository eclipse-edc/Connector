/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"ConstantConditions"})
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
}