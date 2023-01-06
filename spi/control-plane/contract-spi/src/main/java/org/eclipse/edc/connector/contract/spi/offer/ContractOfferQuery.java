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

package org.eclipse.edc.connector.contract.spi.offer;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.query.Criterion;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A query that returns contract offers for the given parameters.
 */
public class ContractOfferQuery {
    private final List<Criterion> assetsCriteria = new ArrayList<>();
    private ClaimToken claimToken;
    private Range range = new Range();
    private URI provider;
    private URI consumer;

    private ContractOfferQuery() {
    }

    public static ContractOfferQuery.Builder builder() {
        return ContractOfferQuery.Builder.newInstance();
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }

    public List<Criterion> getAssetsCriteria() {
        return assetsCriteria;
    }

    public Range getRange() {
        return range;
    }

    public URI getProvider() {
        return provider;
    }

    public URI getConsumer() {
        return consumer;
    }

    public static final class Builder {
        private final ContractOfferQuery instance;

        private Builder() {
            instance = new ContractOfferQuery();
        }

        public static Builder newInstance() {
            return new ContractOfferQuery.Builder();
        }

        public Builder claimToken(ClaimToken claimToken) {
            instance.claimToken = claimToken;
            return this;
        }

        public Builder assetsCriterion(Criterion assetsCriterion) {
            instance.assetsCriteria.add(assetsCriterion);
            return this;
        }

        public Builder assetsCriteria(Collection<Criterion> assetsCriteria) {
            instance.assetsCriteria.addAll(assetsCriteria);
            return this;
        }

        public Builder range(Range range) {
            instance.range = range;
            return this;
        }

        public Builder provider(URI provider) {
            instance.provider = provider;
            return this;
        }

        public Builder consumer(URI consumer) {
            instance.consumer = consumer;
            return this;
        }

        public ContractOfferQuery build() {
            return instance;
        }
    }
}
