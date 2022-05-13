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

package org.eclipse.dataspaceconnector.system.tests.utils;

/**
 * Data passed to {@link TransferSimulationConfiguration#createTransferRequest(TransferInitiationData)}.
 */
public class TransferInitiationData {
    public final String contractAgreementId;
    public final String connectorAddress;

    TransferInitiationData(String contractAgreementId, String connectorAddress) {
        this.contractAgreementId = contractAgreementId;
        this.connectorAddress = connectorAddress;
    }
}
