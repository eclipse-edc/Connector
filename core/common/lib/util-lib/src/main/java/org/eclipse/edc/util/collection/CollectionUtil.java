/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.util.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class CollectionUtil {

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    private static <V, K> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static <T> boolean isAnyOf(T t, T... collection) {
        if (collection == null) {
            return false;
        }

        return Arrays.asList(collection).contains(t);
    }
}
