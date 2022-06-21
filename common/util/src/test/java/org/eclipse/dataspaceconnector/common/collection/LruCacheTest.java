/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.common.collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LruCacheTest {
    private LruCache<String, String> cache;

    @Test
    void verifyEviction() {
        cache.put("foo", "foo");
        cache.put("bar", "bar");
        assertThat(cache.containsKey("foo")).isTrue();
        assertThat(cache.containsKey("bar")).isTrue();

        cache.put("baz", "baz");
        assertThat(cache.containsKey("baz")).isTrue();
        assertThat(cache.containsKey("bar")).isTrue();
        assertThat(cache.containsKey("foo")).isFalse();
    }

    @BeforeEach
    void setUp() {
        cache = new LruCache<>(2);
    }
}
