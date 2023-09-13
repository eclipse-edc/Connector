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

package org.eclipse.edc.connector.asset;

import org.eclipse.edc.connector.core.store.CriterionToPredicateConverterImpl;
import org.eclipse.edc.spi.query.CriterionToAssetPredicateConverter;
import org.eclipse.edc.spi.query.CriterionToPredicateConverter;
import org.eclipse.edc.spi.types.domain.asset.Asset;

/**
 * Extension class that supports converting criterion to predicate looking at the Asset properties.
 */
public class CriterionToAssetPredicateConverterImpl extends CriterionToPredicateConverterImpl implements CriterionToAssetPredicateConverter, CriterionToPredicateConverter {

    public Object property(String key, Object object) {
        if (object instanceof Asset asset) {
            if (asset.getProperties().containsKey(key)) {
                return asset.getProperty(key);
            }
            if (asset.getPrivateProperties().containsKey(key)) {
                return asset.getPrivateProperty(key);
            }

            return super.property(key, object);
        }
        throw new IllegalArgumentException("Can only handle objects of type " + Asset.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
