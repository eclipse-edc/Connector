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

package org.eclipse.edc.connector.controlplane.query.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.query.ReflectionPropertyLookup;
import org.eclipse.edc.spi.query.PropertyLookup;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Map.entry;

public class AssetPropertyLookup implements PropertyLookup {

    private final PropertyLookup fallbackPropertyLookup = new ReflectionPropertyLookup();

    @Override
    public Object getProperty(String key, Object object) {
        if (object instanceof Asset asset) {
            Stream<Map.Entry<String, Function<Asset, Map<String, Object>>>> mappings = Stream.of(
                    entry("%s", Asset::getProperties),
                    entry("'%s'", Asset::getProperties),
                    entry("%s", Asset::getPrivateProperties),
                    entry("'%s'", Asset::getPrivateProperties));

            return mappings
                    .map(entry -> fallbackPropertyLookup.getProperty(entry.getKey().formatted(key), entry.getValue().apply(asset)))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> fallbackPropertyLookup.getProperty(key, asset));
        }

        return null;
    }
}
