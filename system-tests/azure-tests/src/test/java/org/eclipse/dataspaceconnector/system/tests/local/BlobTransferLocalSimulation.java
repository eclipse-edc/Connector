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

package org.eclipse.dataspaceconnector.system.tests.local;

public class BlobTransferLocalSimulation extends TransferLocalSimulation {
    static final String ACCOUNT_NAME_PROPERTY = "BlobTransferLocalSimulation-account-name";

    public BlobTransferLocalSimulation() {
        super(new BlobTransferRequestFactory(System.getProperty(ACCOUNT_NAME_PROPERTY)));
    }
}