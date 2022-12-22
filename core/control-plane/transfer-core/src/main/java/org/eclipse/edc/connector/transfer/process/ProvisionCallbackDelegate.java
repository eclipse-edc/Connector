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

package org.eclipse.edc.connector.transfer.process;

import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * Receives provision callbacks.
 */
public interface ProvisionCallbackDelegate {
    /**
     * Called when the {@link ProvisionManager} completes provisioning.
     */
    Result<Void> handleProvisionResult(String transferProcessId, List<StatusResult<ProvisionResponse>> provisionResponse);

    /**
     * Called when the {@link ProvisionManager} completes deprovisioning.
     */
    Result<Void> handleDeprovisionResult(String transferProcessId, List<StatusResult<DeprovisionedResource>> provisionResponse);
}
