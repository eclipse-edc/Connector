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

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * Cache, that maintains a map of key-value pairs, where values have an individual expiry. When values expire, they are re-fetched
 * and put back in the cache.
 * <p>
 * Values are not stored directly, but are wrapped in a {@link TimestampedValue}. When getting values from the cache, one has to provide
 * a {@code cacheEntryUpdateFunction}, which encapsulates the re-fetching of the expired value.
 * <p>
 * This cache is thread-safe.
 */
public class Cache<K, V> {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<K, TimestampedValue<V>> cache = new HashMap<>();
    private final Function<K, V> cacheEntryUpdateFunction;
    private final long validity;
    private final Clock clock;

    public Cache(Function<K, V> cacheEntryUpdateFunction, long validity) {
        this(cacheEntryUpdateFunction, validity, Clock.systemUTC());
    }

    public Cache(Function<K, V> cacheEntryUpdateFunction, long validity, Clock clock) {
        this.cacheEntryUpdateFunction = cacheEntryUpdateFunction;
        this.validity = validity;
        this.clock = clock;
    }

    /**
     * Gets an entry from the cache, or - if the entry is expired - calls the refresh function, and then gets the value.
     *
     * @param key The key of the value to get.
     * @return the value
     */
    public V get(K key) {
        V value;
        lock.readLock().lock();
        try {
            if (isEntryExpired(key)) {
                lock.readLock().unlock(); // unlock read, acquire write -> "upgrade" lock
                lock.writeLock().lock();
                try {
                    if (isEntryExpired(key)) {
                        var newEntry = cacheEntryUpdateFunction.apply(key);
                        cache.put(key, new TimestampedValue<>(newEntry, Instant.now(), validity));
                    }
                } finally {
                    lock.readLock().lock(); // downgrade lock
                    lock.writeLock().unlock();
                }
            }

            value = cache.get(key).value();
        } finally {
            lock.readLock().unlock();
        }
        return value;
    }

    /**
     * Explicitly removes an entry from the cache
     *
     * @param key the key
     * @return the value previously associated with "key"
     */
    public V evict(K key) {
        lock.writeLock().lock();
        try {
            return ofNullable(cache.remove(key)).map(TimestampedValue::value).orElse(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean isEntryExpired(K key) {
        var timestampedValue = cache.get(key);
        if (timestampedValue == null) return true;
        return timestampedValue.isExpired(clock);
    }


}
