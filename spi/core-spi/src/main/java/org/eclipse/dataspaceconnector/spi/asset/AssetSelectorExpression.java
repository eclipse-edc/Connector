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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.query.Criterion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Selects a group of assets based on the given criteria.
 * <p>
 * If an AssetSelectorExpression does not contain any criteria, no assets are selected. If all Assets are to be selected, the {@link AssetSelectorExpression#SELECT_ALL} constant
 * must be used.
 */
@JsonDeserialize(builder = AssetSelectorExpression.Builder.class)
public final class AssetSelectorExpression {

    /**
     * Constant to select the entire {@link AssetIndex} content. Depending on the implementation,
     * this could take a long time!
     */
    public static final AssetSelectorExpression SELECT_ALL = new AssetSelectorExpression();
    private List<Criterion> criteria;

    private AssetSelectorExpression() {
        criteria = new ArrayList<>();
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }

    @Override
    public int hashCode() {
        return Objects.hash(criteria);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AssetSelectorExpression that = (AssetSelectorExpression) o;
        return criteria == that.criteria || criteria.equals(that.criteria);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final AssetSelectorExpression expression;

        private Builder() {
            expression = new AssetSelectorExpression();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder criteria(List<Criterion> criteria) {
            expression.criteria = criteria;
            return this;
        }

        @JsonIgnore
        public Builder constraint(String left, String op, String right) {
            expression.criteria.add(new Criterion(left, op, right));
            return this;
        }

        /**
         * Convenience method to express equality checks. Is equivalent to {@code Builder.withConstraint(key, "=", value)}
         *
         * @param key   left-hand operand
         * @param value right-hand operand
         */
        @JsonIgnore
        public Builder whenEquals(String key, String value) {
            expression.criteria.add(new Criterion(key, "=", value));
            return this;
        }

        public AssetSelectorExpression build() {
            return expression;
        }
    }

}
