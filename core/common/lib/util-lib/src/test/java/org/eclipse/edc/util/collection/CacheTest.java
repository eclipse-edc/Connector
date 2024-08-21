/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.util.collection;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheTest {
    public static final int VALIDITY = 5 * 60 * 1000;
    private final Function<String, TestObject> updateFunction = mock();
    private Cache<String, TestObject> cache = new Cache<>(updateFunction, VALIDITY, Clock.systemUTC());


    @Test
    void get_whenNotPresent() {
        when(updateFunction.apply(anyString())).thenReturn(new TestObject(42));
        var testObject = cache.get("foo");
        assertThat(testObject.value()).isEqualTo(42);
        verify(updateFunction).apply(anyString());
    }

    @Test
    void get_whenPresent() {
        when(updateFunction.apply(anyString())).thenReturn(new TestObject(42));
        cache.get("foo");
        cache.get("foo");
        cache.get("foo");
        var testObject4 = cache.get("foo");
        assertThat(testObject4.value()).isEqualTo(42);
        verify(updateFunction, times(1)).apply(anyString());
    }

    @Test
    void get_whenPresent_expired() {
        cache = new Cache<>(updateFunction, VALIDITY, Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.systemDefault()));
        when(updateFunction.apply(anyString())).thenReturn(new TestObject(42));

        cache.get("foo"); // no entry there -> expect update

        var testObject4 = cache.get("foo"); //entry present but expired -> expect update
        assertThat(testObject4.value()).isEqualTo(42);
        verify(updateFunction, times(2)).apply(anyString());
    }

    @Test
    void get_whenPresent_expired_updateFails() {
        cache = new Cache<>(s -> null, VALIDITY, Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.systemDefault()));
        when(updateFunction.apply(anyString())).thenReturn(new TestObject(42));

        cache.get("foo"); // no entry there -> expect update

        var testObject = cache.get("foo"); //entry present but expired -> expect update
        assertThat(testObject).isNull();
    }

    @Test
    void evict_whenPresent() {
        when(updateFunction.apply(anyString())).thenReturn(new TestObject(42));
        cache.get("foo");

        assertThat(cache.evict("foo").value).isEqualTo(42);

        var testObject = cache.get("foo");
        assertThat(testObject.value()).isEqualTo(42);
        verify(updateFunction, times(2)).apply(anyString());
    }

    @Test
    void evict_whenNotPresent() {
        when(updateFunction.apply(anyString())).thenReturn(new TestObject(42));

        assertThat(cache.evict("foo")).isNull();

        var testObject = cache.get("foo");
        assertThat(testObject.value()).isEqualTo(42);
        verify(updateFunction, times(1)).apply(anyString());
    }

    private record TestObject(int value) {

    }
}