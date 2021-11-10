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

package org.eclipse.dataspaceconnector.spi.contract;

import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;

/**
 * The {@link ContractOfferFrameworkQuery} narrows down the number of queried {@link ContractOfferTemplate}.
 */
public class ContractOfferFrameworkQuery {

    private VerificationResult verificationResult;

    private ContractOfferFrameworkQuery() {
    }

    public VerificationResult getVerificationResult() {
        return verificationResult;
    }

    public static Builder builder() {
        return Builder.newInstance();
    }

    public static final class Builder {
        private VerificationResult verificationResult;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder verificationResult(final VerificationResult verificationResult) {
            this.verificationResult = verificationResult;
            return this;
        }

        public ContractOfferFrameworkQuery build() {
            final ContractOfferFrameworkQuery contractOfferFrameworkQuery = new ContractOfferFrameworkQuery();
            contractOfferFrameworkQuery.verificationResult = this.verificationResult;
            return contractOfferFrameworkQuery;
        }
    }
}
