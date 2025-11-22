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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactored
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.DeprovisionRequest;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Transitions a transfer process to the {@link TransferProcessStates#DEPROVISIONING DEPROVISIONING} state.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
public class DeprovisionRequestCommandHandler extends EntityCommandHandler<DeprovisionRequest, TransferProcess> {

    public DeprovisionRequestCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<DeprovisionRequest> getType() {
        return DeprovisionRequest.class;
    }

    @Override
    protected boolean modify(TransferProcess process, DeprovisionRequest command) {
        if (process.canBeDeprovisioned()) {
            process.transitionDeprovisioning();
            return true;
        }

        return false;
    }

}
