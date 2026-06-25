/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.store;

import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.util.reflection.ReflectionUtil;

import java.util.Comparator;

/**
 * A object comparator that acts on its fields
 */
public class FieldComparator<T> implements Comparator<T> {

    protected final String fieldName;
    protected final SortOrder sortOrder;

    public FieldComparator(String fieldName, SortOrder sortOrder) {
        this.fieldName = fieldName;
        this.sortOrder = sortOrder;
    }

    @Override
    public int compare(T obj1, T obj2) {
        var o1 = ReflectionUtil.getFieldValue(fieldName, obj1);
        var o2 = ReflectionUtil.getFieldValue(fieldName, obj2);

        if (o1 == null || o2 == null) {
            return 0;
        }

        if (!(o1 instanceof Comparable comp1)) {
            throw new IllegalArgumentException("A property '" + fieldName + "' is not comparable!");
        }
        var comp2 = (Comparable) o2;
        return sortOrder == SortOrder.ASC ? comp1.compareTo(comp2) : comp2.compareTo(comp1);
    }
}
