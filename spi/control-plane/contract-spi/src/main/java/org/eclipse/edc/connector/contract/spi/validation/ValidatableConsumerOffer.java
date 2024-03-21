/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.validation;

import org.eclipse.edc.connector.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.policy.model.Policy;

import static java.util.Objects.requireNonNull;

/**
 * Enriched consumer offer which contains all information for validating the offer with
 * {@link ContractValidationService#validateInitialOffer}
 */
public class ValidatableConsumerOffer {

    private ContractOfferId offerId;
    private ContractDefinition contractDefinition;
    private Policy accessPolicy;
    private Policy contractPolicy;

    private ValidatableConsumerOffer() {
    }

    public Policy getContractPolicy() {
        return contractPolicy;
    }

    public ContractOfferId getOfferId() {
        return offerId;
    }

    public Policy getAccessPolicy() {
        return accessPolicy;
    }

    public ContractDefinition getContractDefinition() {
        return contractDefinition;
    }

    public static final class Builder {

        private final ValidatableConsumerOffer consumerOffer;

        private Builder(ValidatableConsumerOffer consumerOffer) {
            this.consumerOffer = consumerOffer;
        }

        public static Builder newInstance() {
            return new Builder(new ValidatableConsumerOffer());
        }


        public Builder offerId(ContractOfferId offerId) {
            consumerOffer.offerId = offerId;
            return this;
        }

        public Builder contractDefinition(ContractDefinition contractDefinition) {
            consumerOffer.contractDefinition = contractDefinition;
            return this;
        }

        public Builder accessPolicy(Policy accessPolicy) {
            consumerOffer.accessPolicy = accessPolicy;
            return this;
        }

        public Builder contractPolicy(Policy contractPolicy) {
            consumerOffer.contractPolicy = contractPolicy;
            return this;
        }

        public ValidatableConsumerOffer build() {
            requireNonNull(consumerOffer.offerId, "offerId");
            requireNonNull(consumerOffer.contractDefinition, "contractDefinition");
            requireNonNull(consumerOffer.accessPolicy, "accessPolicy");
            requireNonNull(consumerOffer.contractPolicy, "contractPolicy");

            return consumerOffer;
        }
    }
}
