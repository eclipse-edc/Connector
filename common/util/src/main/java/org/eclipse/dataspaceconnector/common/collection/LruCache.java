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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache with a specified capacity.
 *
 * N.B. This class is not threadsafe.
 */
public class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    /**
     * Constructor.
     *
     * @param capacity the maximum cache capacity before the olded entry is evicted.
     */
    public LruCache(int capacity) {
        super(capacity + 1, 1, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
