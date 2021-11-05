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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AssetSelectorExpressions are using labels to qualify a subset of all Assets in the {@link AssetIndex}.
 * Mostly, this will be used in the
 * {@link org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework}.
 * <p>
 * If an AssetSelectorExpression does not contain any criteria, it selects no Assets. If all Assets are to be selected,
 * use the {@link AssetSelectorExpression#SELECT_ALL}.
 */
@JsonDeserialize(builder = AssetSelectorExpression.Builder.class)
public final class AssetSelectorExpression {

    /**
     * Constant to select the entire {@link AssetIndex} content. Depending on the implementation,
     * this could take a long time!
     */
    public static final AssetSelectorExpression SELECT_ALL = AssetSelectorExpression.Builder.newInstance()
            .whenEquals("*", "*").build();

    /**
     * Criteria to retrieve a subset of {@link org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset}s from the {@link AssetIndex}.
     */
    private final List<Criterion> criteria;

    private AssetSelectorExpression(final List<Criterion> criteria) {
        this.criteria = criteria;
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

        private final List<Criterion> criteria;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            criteria = new ArrayList<>();
        }

        public Builder criteria(List<Criterion> criteria) {
            this.criteria.addAll(criteria);
            return this;
        }

        @JsonIgnore
        public Builder constraint(String left, String op, String right) {
            this.criteria.add(new Criterion(left, op, right));
            return this;
        }

        /**
         * Convenience method to express equality checks. Is equivalent to
         * {@code Builder.withConstraint(key, "=", value)}
         *
         * @param key   left-hand operand
         * @param value right-hand operand
         */
        @JsonIgnore
        public Builder whenEquals(String key, String value) {
            this.criteria.add(new Criterion(key, "=", value));
            return this;
        }

        public AssetSelectorExpression build() {
            return new AssetSelectorExpression(criteria);
        }
    }
}
