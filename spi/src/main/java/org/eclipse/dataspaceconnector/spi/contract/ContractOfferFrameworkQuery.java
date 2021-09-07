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
 * The {@link ContractOfferFrameworkQuery} narrows down the number of queried {@link ContractOfferTemplate}.
 */
public class ContractOfferFrameworkQuery {

    // TODO define who the principal is and rename it (promisee, assigne, recipient, daps token, etc.?)
    private Principal principal;

    private ContractOfferFrameworkQuery() {
    }

    public Principal getPrincipal() {
        return principal;
    }

    public static Builder builder() {
        return Builder.newInstance();
    }

    public static final class Builder {
        private Principal principal;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder principal(final Principal principal) {
            this.principal = principal;
            return this;
        }

        public ContractOfferFrameworkQuery build() {
            final ContractOfferFrameworkQuery contractOfferFrameworkQuery = new ContractOfferFrameworkQuery();
            contractOfferFrameworkQuery.principal = this.principal;
            return contractOfferFrameworkQuery;
        }
    }
}
