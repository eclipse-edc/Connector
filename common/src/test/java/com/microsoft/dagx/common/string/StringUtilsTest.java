/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.string;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
class StringUtilsTest {

    @Test
    void testEquals() {
        assertThat(StringUtils.equals("", "")).isTrue();
        assertThat(StringUtils.equals("", null)).isFalse();
        assertThat(StringUtils.equals("", " ")).isFalse();
        assertThat(StringUtils.equals("foo", "fOo")).isFalse();
        assertThat(StringUtils.equals("foo", "fo o")).isFalse();
        assertThat(StringUtils.equals("foo", "Foo")).isFalse();
        assertThat(StringUtils.equals(null, "")).isFalse();
        assertThat(StringUtils.equals(null, null)).isTrue();
    }

    @Test
    void isNullOrEmpty() {
        assertThat(StringUtils.isNullOrEmpty("")).isTrue();
        assertThat(StringUtils.isNullOrEmpty("  ")).isFalse();
        assertThat(StringUtils.isNullOrEmpty(null)).isTrue();
        assertThat(StringUtils.isNullOrEmpty("foobar")).isFalse();
    }

    @Test
    void isNullOrBlank() {
        assertThat(StringUtils.isNullOrBlank("")).isTrue();
        assertThat(StringUtils.isNullOrBlank("  ")).isTrue();
        assertThat(StringUtils.isNullOrBlank(null)).isTrue();
        assertThat(StringUtils.isNullOrBlank("foobar")).isFalse();
    }

    @Test
    void equalsIgnoreCase() {
        assertThat(StringUtils.equalsIgnoreCase("", "")).isTrue();
        assertThat(StringUtils.equalsIgnoreCase("", null)).isFalse();
        assertThat(StringUtils.equalsIgnoreCase("", " ")).isFalse();
        assertThat(StringUtils.equalsIgnoreCase("foo", "fOo")).isTrue();
        assertThat(StringUtils.equalsIgnoreCase("foo", "fo o")).isFalse();
        assertThat(StringUtils.equalsIgnoreCase("foo", "Foo")).isTrue();
        assertThat(StringUtils.equalsIgnoreCase(null, "")).isFalse();
        assertThat(StringUtils.equalsIgnoreCase(null, null)).isTrue();
        assertThat(StringUtils.equalsIgnoreCase("FOO", "fOo")).isTrue();

    }
}