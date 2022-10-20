/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.defaults.storage.transferprocess;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;

class TestFunctions {

    public static TransferProcess createProcess(String name) {
        DataRequest mock = DataRequest.Builder.newInstance().destinationType("type").build();
        return TransferProcess.Builder.newInstance()
                .type(TransferProcess.Type.CONSUMER)
                .id(name)
                .stateTimestamp(0)
                .state(TransferProcessStates.UNSAVED.code())
                .provisionedResourceSet(new ProvisionedResourceSet())
                .dataRequest(mock)
                .build();
    }
}
