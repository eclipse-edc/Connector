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

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;

import java.util.List;

/**
 * Receives provision callbacks.
 */
public interface ProvisionCallbackDelegate {
    /**
     * Called when the {@link org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager} completes provisioning.
     */
    void handleProvisionResult(String transferProcessId, List<StatusResult<ProvisionResponse>> provisionResponse);

    /**
     * Called when the {@link org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager} completes deprovisioning.
     */
    void handleDeprovisionResult(String transferProcessId, List<StatusResult<DeprovisionedResource>> provisionResponse);
}
