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

import org.eclipse.edc.spi.types.domain.offer.ContractOffer;

import static java.util.Objects.requireNonNull;

/**
 * A validated contract offer received from a client. Specifically, the consumer identity and offer have been determined to be
 * trustworthy and correct.
 */
public class ValidatedConsumerOffer {
    private final String consumerIdentity;
    private final ContractOffer offer;

    public ValidatedConsumerOffer(String consumerIdentity, ContractOffer offer) {
        requireNonNull(consumerIdentity, "clientId");
        requireNonNull(offer, "offer");
        this.consumerIdentity = consumerIdentity;
        this.offer = offer;
    }

    public String getConsumerIdentity() {
        return consumerIdentity;
    }

    public ContractOffer getOffer() {
        return offer;
    }

}
