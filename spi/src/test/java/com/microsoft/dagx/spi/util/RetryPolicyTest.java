/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.util;

import com.microsoft.dagx.spi.DagxException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RetryPolicyTest {

    @Test
    void execute_noExceptionHappens() {
        var rp = RetryPolicy.create()
                .withMaxRetries(3)
                .withTimeoutMillis(2)
                .execute(() -> 16);
        assertThat(rp).isEqualTo(16);
    }

    @Test
    void execute_resolvedInMinRetries() {
        AtomicInteger invocations = new AtomicInteger();
        int maxRetries = 3;
        long d = System.currentTimeMillis();

        var rp = RetryPolicy.create()
                .withMaxRetries(maxRetries)
                .withTimeout(500, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (invocations.get() < maxRetries - 1) {
                        invocations.getAndIncrement();
                        throw new IllegalArgumentException();
                    }
                    return 42;
                });
        assertThat(rp).isEqualTo(42);
        assertThat(invocations).hasValue(2);
        // assert that at least 1500 milliseconds (500+1000) have passed
        assertThat(System.currentTimeMillis() - d).isGreaterThanOrEqualTo(1500);
    }

    @Test
    void execute_throws() {
        AtomicInteger invocations = new AtomicInteger();
        int maxRetries = 2;

        ThrowableAssert.ThrowingCallable tc = () -> RetryPolicy.create()
                .withMaxRetries(maxRetries)
                .withTimeout(2, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    invocations.incrementAndGet();
                    throw new IllegalArgumentException();
                });

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(tc);

        assertThat(invocations).hasValue(maxRetries);
    }

    @Test
    void execute_excluededExceptionHappens() {
        AtomicInteger invocations = new AtomicInteger();

        ThrowableAssert.ThrowingCallable tc = () -> RetryPolicy.create()
                .withTimeout(2, TimeUnit.MILLISECONDS)
                .excluding(DagxException.class)
                .execute(() -> {
                    if (invocations.get() > 0) {
                        throw new DagxException("foobar"); //should blow up right away
                    } else {
                        invocations.incrementAndGet();
                        throw new IllegalArgumentException();
                    }
                });

        assertThatExceptionOfType(DagxException.class)
                .isThrownBy(tc);
    }
}