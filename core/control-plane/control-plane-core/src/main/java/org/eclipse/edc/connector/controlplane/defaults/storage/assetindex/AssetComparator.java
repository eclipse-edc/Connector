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

package org.eclipse.edc.connector.controlplane.defaults.storage.assetindex;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.store.FieldComparator;
import org.jetbrains.annotations.Nullable;

/**
 * Comparator for Assets. It looks for "properties" and "private properties", if no one is found, it falls back to the
 * {@link FieldComparator}
 */
public final class AssetComparator extends FieldComparator<Asset> {

    AssetComparator(String sortField, SortOrder sortOrder) {
        super(sortField, sortOrder);
    }

    @Override
    public int compare(Asset asset1, Asset asset2) {
        var f1 = asComparable(asset1.getPropertyOrPrivate(fieldName));
        var f2 = asComparable(asset2.getPropertyOrPrivate(fieldName));

        if (f1 == null || f2 == null) {
            return super.compare(asset1, asset2);
        }
        return sortOrder == SortOrder.ASC ? f1.compareTo(f2) : f2.compareTo(f1);
    }

    private @Nullable Comparable<Object> asComparable(Object property) {
        return property instanceof Comparable<?> comparable ? (Comparable<Object>) comparable : null;
    }

}
