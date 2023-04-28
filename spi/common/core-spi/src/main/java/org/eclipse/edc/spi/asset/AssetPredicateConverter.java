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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.spi.asset;

import org.eclipse.edc.spi.query.BaseCriterionToPredicateConverter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;

/**
 * Converts a {@link Criterion}, which is essentially a select statement, into a {@code Predicate<Asset>}.
 * <p>
 * This is useful when dealing with in-memory collections of objects, here: {@link Asset} where Predicates can be applied
 * efficiently.
 * <p>
 * _Note: other {@link AssetIndex} implementations might have different converters!
 */
public class AssetPredicateConverter extends BaseCriterionToPredicateConverter<Asset> {

    @Override
    public Object property(String key, Object object) {
        if (object instanceof Asset) {
            var asset = (Asset) object;
            boolean emptyProperties = asset.getProperties() == null || asset.getProperties().isEmpty();
            boolean emptyPrivateProperties = asset.getPrivateProperties() == null || asset.getPrivateProperties().isEmpty();
            if (!emptyProperties) {
                if (asset.getProperties().containsKey(key)){
                    return asset.getProperty(key);
                }
            }
            if (!emptyPrivateProperties) {
                if (asset.getPrivateProperties().containsKey(key)) {
                    return asset.getPrivateProperty(key);
                }
            }
            return null;
        }
        throw new IllegalArgumentException("Can only handle objects of type " + Asset.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
