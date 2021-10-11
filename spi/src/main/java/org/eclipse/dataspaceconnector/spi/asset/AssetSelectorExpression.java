/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.asset;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Labels are used to trim down the selection of assets. If the carried hash
 * of labels is empty all available assets are eligible for selection.
 */
public final class AssetSelectorExpression {

    private List<Predicate<Asset>> filters;

    public AssetSelectorExpression() {
        filters = new ArrayList<>();
    }

    public List<Predicate<Asset>> getFilters() {
        return filters;
    }

    public static final class Builder {
        private final AssetSelectorExpression expression;

        private Builder() {
            expression = new AssetSelectorExpression();

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder filters(List<Predicate<Asset>> filters) {
            expression.filters = filters;
            return this;
        }

        public Builder withFilter(Predicate<Asset> predicate) {
            expression.filters.add(predicate);
            return this;
        }

        public Builder withFilter(String fieldName, Object equalsValue) {
            Predicate<Asset> pred = (asset) -> {
                try {
                    return Objects.equals(asset.getClass().getDeclaredField(fieldName).get(asset), equalsValue);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            };

            return withFilter(pred);
        }

        public AssetSelectorExpression build() {
            return expression;
        }
    }
}
