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
 *       Microsoft Corporation - Refactoring
 *
 */

package org.eclipse.dataspaceconnector.spi.contract.offer;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.query.Criterion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A query that returns contract offers for the given parameters.
 */
public class ContractOfferQuery {
    private ClaimToken claimToken;
    private List<Criterion> criteria;
    private Range range;

    private ContractOfferQuery() {
    }

    public static ContractOfferQuery.Builder builder() {
        return ContractOfferQuery.Builder.newInstance();
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public Range getRange() {
        return range;
    }

    public static final class Builder {
        private final List<Criterion> criteria = new ArrayList<>();
        private ClaimToken claimToken;
        private Range range;

        private Builder() {
        }

        public static Builder newInstance() {
            return new ContractOfferQuery.Builder();
        }

        public Builder claimToken(ClaimToken claimToken) {
            this.claimToken = claimToken;
            return this;
        }

        public Builder criterion(Criterion criterion) {
            criteria.add(criterion);
            return this;
        }

        public Builder criteria(Collection<Criterion> criteria) {
            this.criteria.addAll(criteria);
            return this;
        }

        public Builder range(Range range) {
            this.range = range;
            return this;
        }

        public ContractOfferQuery build() {
            ContractOfferQuery contractOfferQuery = new ContractOfferQuery();
            contractOfferQuery.claimToken = claimToken;
            contractOfferQuery.range = range;
            contractOfferQuery.criteria = criteria;
            return contractOfferQuery;
        }
    }
}
