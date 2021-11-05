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

import java.security.Principal;

/**
 * The {@link ContractOfferFrameworkQuery} narrows down the number of
 * queried {@link org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer}.
 */
public class ContractOfferQuery {

    // TODO: decide whether either use org.eclipse.dataspaceconnector.spi.iam.VerificationResult or introduce
    //       container principal object wrapping org.eclipse.dataspaceconnector.spi.iam.VerificationResult
    private Principal principal;

    private ContractOfferQuery() {
    }

    public Principal getPrincipal() {
        return principal;
    }


    public static final class Builder {
        private Principal principal;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder principal(final Principal principal) {
            this.principal = principal;
            return this;
        }

        public ContractOfferQuery build() {
            final ContractOfferQuery contractOfferQuery = new ContractOfferQuery();
            contractOfferQuery.principal = this.principal;
            return contractOfferQuery;
        }
    }
}