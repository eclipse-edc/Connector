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
 *       Mercedes-Benz Tech Innovation GmbH - connector id removal
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class TestFunctions {

    public static TransferProcess createTransferProcess() {
        return createTransferProcess("test-process");
    }

    public static TransferProcess createTransferProcess(String processId, TransferProcessStates state) {
        return createTransferProcessBuilder(processId).state(state.code()).build();
    }

    public static TransferProcess createTransferProcess(String processId) {
        return createTransferProcessBuilder(processId)
                .state(TransferProcessStates.INITIAL.code())
                .build();
    }

    public static TransferProcess.Builder createTransferProcessBuilder(String processId) {
        return TransferProcess.Builder.newInstance()
                .id(processId)
                .createdAt(Clock.systemUTC().millis())
                .state(TransferProcessStates.INITIAL.code())
                .type(TransferProcess.Type.CONSUMER)
                .contentDataAddress(createDataAddressBuilder("any").build())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("local://test").build()))
                .participantContextId("participantContextId");
    }

    public static DataAddress.Builder createDataAddressBuilder(String type) {
        return DataAddress.Builder.newInstance()
                .type(type);
    }

    @NotNull
    public static TransferProcess initialTransferProcess() {
        return createTransferProcessBuilder(UUID.randomUUID().toString()).correlationId("clientid").build();
    }

}
