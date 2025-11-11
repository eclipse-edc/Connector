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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.command;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.command.EntityCommand;

/**
 * Issues a request to start deprovisioning a transfer process by setting its state to
 * {@link TransferProcessStates#DEPROVISIONING DEPROVISIONING}.
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
public class DeprovisionRequest extends EntityCommand {

    public DeprovisionRequest(String transferProcessId) {
        super(transferProcessId);
    }
}
