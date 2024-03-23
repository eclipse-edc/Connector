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

package org.eclipse.edc.util.reflection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PathItemTest {

    @Test
    void shouldParse_singleElement() {
        var result = PathItem.parse("test");

        assertThat(result).hasSize(1).first().extracting(PathItem::toString).isEqualTo("test");
    }

    @Test
    void shouldParse_singleWrappedElement() {
        var result = PathItem.parse("'test.data'");

        assertThat(result).hasSize(1).first().extracting(PathItem::toString).isEqualTo("test.data");
    }

    @Test
    void shouldParse_multipleSeparatedByDots() {
        var result = PathItem.parse("test.data.path");

        assertThat(result).hasSize(3).extracting(PathItem::toString)
                .containsExactly("test", "data", "path");
    }

    @Test
    void shouldParse_multipleWrappedSeparatedByDots() {
        var result = PathItem.parse("'test.test'.data.'path.path'");

        assertThat(result).hasSize(3).extracting(PathItem::toString)
                .containsExactly("test.test", "data", "path.path");
    }
}
