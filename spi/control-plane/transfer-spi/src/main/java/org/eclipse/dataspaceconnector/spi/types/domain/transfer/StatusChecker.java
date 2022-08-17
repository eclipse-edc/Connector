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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import java.util.List;

/**
 * Checks if a data transfer has completed.
 * <p>
 * For example, a data transfer type involving the copying of files to a folder may employ a completion check that verifies if a file with the "*.complete" extension exists.
 */
@FunctionalInterface
public interface StatusChecker {

    /**
     * Checks if the data transfer is finished.
     *
     * @param transferProcess The data transfer process id
     * @param resources       The provisioned resources associated with the transfer or an empty collection if no resources were provisioned
     * @return true if complete; otherwise false
     */
    boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources);
}
