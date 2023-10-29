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

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Map.entry;

/**
 * Extension class that supports converting criterion to predicate looking at the Asset properties.
 */
public class CriterionToAssetPredicateConverterImpl extends CriterionToPredicateConverterImpl implements CriterionToAssetPredicateConverter, CriterionToPredicateConverter {

    public Object property(String key, Object object) {
        if (object instanceof Asset asset) {
            Stream<Map.Entry<String, Function<Asset, Map<String, Object>>>> mappings = Stream.of(
                    entry("%s", Asset::getProperties),
                    entry("'%s'", Asset::getProperties),
                    entry("%s", Asset::getPrivateProperties),
                    entry("'%s'", Asset::getPrivateProperties));

            return mappings
                    .map(entry -> super.property(entry.getKey().formatted(key), entry.getValue().apply(asset)))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> super.property(key, asset));
        }
        throw new IllegalArgumentException("Can only handle objects of type " + Asset.class.getSimpleName() + " but received an " + object.getClass().getSimpleName());
    }
}
