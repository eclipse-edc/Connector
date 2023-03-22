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

package org.eclipse.edc.test.system.utils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Orchestrate a transfer test with {@link TransferConfiguration} using the test client {@link TransferTestClient}.
 * It fetches the catalog, for each offer
 */
public class TransferTestRunner {

    private final TransferConfiguration configuration;

    public TransferTestRunner(TransferConfiguration configuration) {
        this.configuration = configuration;
    }

    public void executeTransfer() {
        var client = new TransferTestClient(configuration.getConsumerManagementUrl());
        var catalog = client.getCatalog(configuration.getProviderIdsUrl());

        for (var offer : catalog.getContractOffers()) {
            var contractRequest = configuration.createNegotiationRequest(offer);
            var contractAgreement = client.negotiateContractAgreement(contractRequest);
            var tr = configuration.createTransferRequest(offer, contractAgreement);
            var transferRequest = client.initiateTransfer(tr);

            assertThat(configuration.isTransferResultValid(transferRequest)).isTrue();
        }
    }
}
