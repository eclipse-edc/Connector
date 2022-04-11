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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.spi;

/**
 * Type of Data Plane transfer.
 */
public interface DataPlaneTransferType {
    /**
     * Synchronous data transfer using public API of the Data Plane as http proxy to query the data.
     */
    String SYNC = "HttpProxy";
}
