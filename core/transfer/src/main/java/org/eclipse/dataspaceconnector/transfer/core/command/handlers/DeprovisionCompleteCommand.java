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

package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.SingleTransferProcessCommand;

/**
 * Informs the system that deprovisioning a resource has indeed completed.
 */
public class DeprovisionCompleteCommand extends SingleTransferProcessCommand {
    private final DeprovisionedResource resource;

    public DeprovisionCompleteCommand(String transferProcessId, DeprovisionedResource resource) {
        super(transferProcessId);
        this.resource = resource;
    }

    public DeprovisionedResource getResource() {
        return resource;
    }
}
