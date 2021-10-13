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

import java.util.ArrayList;
import java.util.List;

/**
 * AssetSelectorExpressions are using labels to qualify a subset of all Assets in the {@link AssetIndex}.
 * Mostly, this will be used in the
 * {@link org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework}.
 * <p>
 * If an AssetSelectorExpression does not contain any labels, it selects no Assets. If all Assets are to be selected,
 * the {@link AssetSelectorExpression#SELECT_ALL} constant must be used.
 * <p>
 * The AssetSelectorExpression should not be used to dynamically narrow a search for assets!
 */
public final class AssetSelectorExpression {

    private List<Criterion> criteria;

    private AssetSelectorExpression() {
        criteria = new ArrayList<>();
    }


    public List<Criterion> getCriteria() {
        return criteria;
    }

    public static final class Builder {
        private final AssetSelectorExpression expression;

        private Builder() {
            expression = new AssetSelectorExpression();

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder filters(List<Criterion> criteria) {
            expression.criteria = criteria;
            return this;
        }

        public Builder withConstraint(String left, String op, String right) {
            expression.criteria.add(new Criterion(left, op, right));
            return this;
        }

        public Builder whenEquals(String key, String value) {
            expression.criteria.add(new Criterion(key, "=", value));
            return this;
        }

        public AssetSelectorExpression build() {
            return expression;
        }
    }

    public static final AssetSelectorExpression SELECT_ALL = new AssetSelectorExpression();

}
