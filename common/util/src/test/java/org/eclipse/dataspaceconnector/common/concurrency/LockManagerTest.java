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

package org.eclipse.dataspaceconnector.common.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.assertj.core.api.Assertions.assertThat;

class LockManagerTest {

    @Test
    void verifyReadAndWriteLock() {
        var lockManager = new LockManager(new ReentrantReadWriteLock());
        var counter = new AtomicInteger();

        lockManager.readLock(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);

        lockManager.writeLock(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void verifyTimeoutOnWriteLockAttempt() {
        var lockManager = new LockManager(new ReentrantReadWriteLock(), 10);
        var counter = new AtomicInteger();

        var latch = new CountDownLatch(1);

        // Attempt to acquire a write lock in another thread, which should timeout as the current thread holds a read lock
        var thread = new Thread(() -> {
            try {
                lockManager.writeLock(() -> {
                    throw new AssertionError();  // lock should never be acquired
                });
            } catch (LockException e) {
                latch.countDown(); // should timeout, release the latch
            }
        });

        lockManager.readLock(() -> {
            try {
                thread.start();
                assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();    // latch to be released after the write lock successfully times out
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                throw new AssertionError();
            }
            return null;
        });

        assertThat(counter.get()).isEqualTo(1);
    }
}
