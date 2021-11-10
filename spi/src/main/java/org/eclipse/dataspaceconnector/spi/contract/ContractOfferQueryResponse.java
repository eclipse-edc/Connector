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

import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;

import java.util.stream.Stream;

// TODO: add pagination attributes

/**
 * The {@link ContractOfferQueryResponse} returns a stream of {@link ContractOffer} as a result of a query.
 */
public class ContractOfferQueryResponse {
    private final Stream<ContractOffer> contractOfferStream;

    public ContractOfferQueryResponse(Stream<ContractOffer> contractOfferStream) {
        this.contractOfferStream = contractOfferStream;
    }

    public Stream<ContractOffer> getContractOfferStream() {
        return contractOfferStream;
    }
}
