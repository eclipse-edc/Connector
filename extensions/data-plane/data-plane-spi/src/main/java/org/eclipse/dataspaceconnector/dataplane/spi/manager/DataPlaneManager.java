/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.spi.manager;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

/**
 * Manages the execution of data plane requests.
 */
public interface DataPlaneManager {

    /**
     * Initiates a data flow request. This method is non-blocking with respect to processing the request.
     */
    void initiateTransfer(DataFlowRequest dataRequest);
}
