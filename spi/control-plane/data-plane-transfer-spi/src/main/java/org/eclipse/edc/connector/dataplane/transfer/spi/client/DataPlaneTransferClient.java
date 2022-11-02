/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.spi.client;

import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

/**
 * Client to interact with Data Plane.
 */
@FunctionalInterface
public interface DataPlaneTransferClient {

    /**
     * Validate incoming data request and, if valid, initiates data transfer.
     */
    StatusResult<Void> transfer(DataFlowRequest request);
}
