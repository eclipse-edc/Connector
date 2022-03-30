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
package org.eclipse.dataspaceconnector.spi.types.domain.transfer.command;

/**
 * Issues a request to start deprovisioning a transfer process by setting its state to
 * {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates#DEPROVISIONING DEPROVISIONING}.
 */
public class DeprovisionRequest extends SingleTransferProcessCommand {

    public DeprovisionRequest(String transferProcessId) {
        super(transferProcessId);
    }
}
