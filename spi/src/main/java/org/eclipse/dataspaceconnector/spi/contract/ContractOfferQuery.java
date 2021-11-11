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
 */

package org.eclipse.dataspaceconnector.spi.contract;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

/**
 * A query that returns contract offers for the given parameters.
 *
 * TODO: add pagination attributes. These should be a numeric skip value and a limit size.
 */
public class ContractOfferQuery {
    private ClaimToken claimToken;

    private ContractOfferQuery() {
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }

    public static ContractOfferQuery.Builder builder() {
        return ContractOfferQuery.Builder.newInstance();
    }

    public static final class Builder {
        private ClaimToken claimToken;

        private Builder() {
        }

        public static Builder newInstance() {
            return new ContractOfferQuery.Builder();
        }

        public Builder claimToken(ClaimToken claimToken) {
            this.claimToken = claimToken;
            return this;
        }

        public ContractOfferQuery build() {
            final ContractOfferQuery contractOfferQuery = new ContractOfferQuery();
            contractOfferQuery.claimToken = this.claimToken;
            return contractOfferQuery;
        }
    }
}
