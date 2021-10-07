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

package org.eclipse.dataspaceconnector.common.string;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void toStringTest() { //cannot be named "toString()"
        assertThat(StringUtils.toString("")).isEqualTo("");
        assertThat(StringUtils.toString(23)).isEqualTo("23");
        assertThat(StringUtils.toString(null)).isEqualTo(null);
        assertThat(StringUtils.toString(new Object())).contains("java.lang.Object@");
    }
}
