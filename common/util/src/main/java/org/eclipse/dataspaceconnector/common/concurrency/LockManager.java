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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

/**
 * Handles acquiring and releasing read and write locks.
 */
public class LockManager {
    private static final int DEFAULT_TIMEOUT = 1000;

    private final ReadWriteLock lock;
    private final int timeout;

    /**
     * Constructor. Uses the default timeout specified by {@link #DEFAULT_TIMEOUT}.
     */
    public LockManager(ReadWriteLock lock) {
        this(lock, DEFAULT_TIMEOUT);
    }

    /**
     * Constructor.
     *
     * @param lock the backing lock.
     * @param timeout the timeout this instance will wait for a lock in milliseconds.
     */
    public LockManager(ReadWriteLock lock, int timeout) {
        this.lock = lock;
        this.timeout = timeout;
    }

    /**
     * Attempts to obtain a read lock.
     */
    public <T> T readLock(Supplier<T> work) {
        try {
            if (!lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                throw new LockException("Timeout acquiring read lock");
            }
            try {
                return work.get();
            } finally {
                lock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Attempts to obtain a write lock.
     */
    public <T> T writeLock(Supplier<T> work) {
        try {
            if (!lock.writeLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                throw new LockException("Timeout acquiring write lock");
            }
            try {
                return work.get();
            } finally {
                lock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new LockException(e);
        }
    }
}
