/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */
package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.command.commands.DeprovisionRequest;

public class DeprovisionRequestHandler extends SingleTransferProcessCommandHandler<DeprovisionRequest> {

    public DeprovisionRequestHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<DeprovisionRequest> getType() {
        return DeprovisionRequest.class;
    }

    @Override
    protected boolean modify(TransferProcess process) {
        process.transitionDeprovisioning();
        return true;
    }
}
