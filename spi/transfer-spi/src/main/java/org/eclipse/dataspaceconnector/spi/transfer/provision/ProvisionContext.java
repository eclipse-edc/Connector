/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.jetbrains.annotations.Nullable;

/**
 * Execution context used by {@link Provisioner}s.
 */
public interface ProvisionContext {

    /**
     * Invoked when a provision request has completed.
     */
    void callback(ProvisionedResource resource);

    /**
     * Invoked when a provision request has completed.
     */
    void callback(ProvisionedDataDestinationResource resource, @Nullable SecretToken secretToken);

    /**
     * Invoked when the deprovisioning has completed.
     *
     * @param resource The resource that was deprovisioned
     * @param error    If an error happened during deprovisioning.
     */
    void deprovisioned(ProvisionedDataDestinationResource resource, Throwable error);

}
