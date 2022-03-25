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
package org.eclipse.dataspaceconnector.transfer.core.command.commands;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;

/**
 * Handles a {@link ProvisionResponse} received from an external system.
 *
 * Some provisioning responses will be asynchronously received in-process, for example, when a provisioner executes locally and returns a response via a future. In contrast,
 * a provisioner that delegates to an external system may receive a response on an asynchronous callback channel. The response therefore cannot be returned using the
 * provisioner's future as there is no guarantee the response from the external system will be routed to the originating EDC runtime.
 *
 * To handle this case, the callback channel will receive the response and submit it via a {@link SingleTransferProcessCommand} and routing to the originating runtime is not
 * required.
 */
public class AddProvisionedResourceCommand extends SingleTransferProcessCommand {
    private ProvisionResponse provisionResponse;

    public AddProvisionedResourceCommand(String transferProcessId, ProvisionResponse provisionedResource) {
        super(transferProcessId);
        this.provisionResponse = provisionedResource;
    }

    public ProvisionResponse getProvisionResponse() {
        return provisionResponse;
    }
}
