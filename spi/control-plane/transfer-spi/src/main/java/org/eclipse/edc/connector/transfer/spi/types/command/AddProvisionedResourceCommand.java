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

package org.eclipse.edc.connector.transfer.spi.types.command;

import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.spi.command.SingleEntityCommand;

/**
 * Handles a {@link ProvisionResponse} received from an external system.
 * <p>
 * Some provisioning responses will be asynchronously received in-process, for example, when a provisioner executes locally and returns a response via a future. In contrast,
 * a provisioner that delegates to an external system may receive a response on an asynchronous callback channel. The response therefore cannot be returned using the
 * provisioner's future as there is no guarantee the response from the external system will be routed to the originating EDC runtime.
 * <p>
 */
public class AddProvisionedResourceCommand extends SingleEntityCommand {
    private final ProvisionResponse provisionResponse;

    public AddProvisionedResourceCommand(String transferProcessId, ProvisionResponse provisionedResource) {
        super(transferProcessId);
        provisionResponse = provisionedResource;
    }

    public ProvisionResponse getProvisionResponse() {
        return provisionResponse;
    }
}
