/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.samples;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * Client application for performing a file transfer
 */
@IntegrationTest
public class FileTransferAsClientIntegrationTest {

    @Test
    public void performFileTransfer() {

        var client = new FileTransferTestUtils();
        client.setConsumerUrl(get("CONSUMER_URL"));
        client.setProviderUrl(get("PROVIDER_URL"));
        client.setDestinationPath(get("DESTINATION_PATH"));
        client.setApiKey(get("API_KEY"));

        var contractAgreementId = client.negotiateContractAgreement();
        client.performFileTransfer(contractAgreementId);
    }

    private static String get(String value) {
        return Objects.requireNonNull(System.getenv(value), "Environment variable " + value + " not set");
    }
}
