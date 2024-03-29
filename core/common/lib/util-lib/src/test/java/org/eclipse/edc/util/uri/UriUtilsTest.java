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

package org.eclipse.edc.util.uri;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class UriUtilsTest {

    @Test
    void verifyEquality() {
        var u1 = URI.create("https://some.url/path/foo#position1");
        var u2 = URI.create("https://some.url/path/foo");
        assertThat(UriUtils.equalsIgnoreFragment(u1, u2)).isTrue();

        var u3 = URI.create("https://some.url/path");
        assertThat(UriUtils.equalsIgnoreFragment(u1, u3)).isFalse();
        assertThat(UriUtils.equalsIgnoreFragment(u2, u3)).isFalse();
    }
}