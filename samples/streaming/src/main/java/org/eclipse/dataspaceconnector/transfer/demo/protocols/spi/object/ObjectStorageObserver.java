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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.object;

import org.eclipse.dataspaceconnector.transfer.demo.protocols.common.DataDestination;

/**
 * Observes storage operations.
 */
public interface ObjectStorageObserver {

    /**
     * Callback when a provision operation succeeds.
     */
    void onProvision(DataDestination dataDestination);

    /**
     * Callback when a deprovision operation completes.
     */
    void onDeprovision(String key);

    /**
     * Callback when a storage operation is invoked.
     */
    void onStore(String containerName, String objectKey, String token, byte[] data);

}
