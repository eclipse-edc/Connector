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

package org.eclipse.edc.util.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe LRU cache with a specified capacity.
 * <p>
 * This class extends {@link LinkedHashMap} and adds concurrency using a ReentrantReadWriteLock.
 * The cache uses a LinkedHashMap to store the entries and automatically evicts the least recently used entry
 * when the capacity is reached.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class ConcurrentLruCache<K, V> extends LinkedHashMap<K, V> {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final int capacity;

    public ConcurrentLruCache(int capacity) {
        super(capacity + 1, 1, true);
        this.capacity = capacity;
    }

    @Override
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return super.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(Object key) {
        lock.writeLock().lock();
        try {
            return super.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        lock.writeLock().lock();
        try {
            return super.remove(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Object clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        lock.readLock().lock();
        try {
            return super.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            super.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
