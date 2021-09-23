/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transfer.functions.core.flow.local;

import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.functions.spi.flow.local.LocalTransferFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Delegates data transfer to a local instance. Local instances can initiate a transfer directly (i.e. in the current process) or invoke a sidecar deployment or remote system
 * using a proprietary protocol.
 */
public class LocalFunctionDataFlowController implements DataFlowController {
    private Set<String> protocols;
    private LocalTransferFunction localTransferFunction;

    public LocalFunctionDataFlowController(Set<String> protocols, LocalTransferFunction localTransferFunction) {
        this.protocols = protocols;
        this.localTransferFunction = localTransferFunction;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return protocols.contains(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        var response = localTransferFunction.initiateFlow(dataRequest);
        return response == ResponseStatus.OK ? DataFlowInitiateResponse.OK : new DataFlowInitiateResponse(response, "Error attempting to transfer data");
    }
}
