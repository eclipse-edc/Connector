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
public interface DataPlaneTransferConstants {
    /**
     * {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest} type that
     * triggers data proxy transfer.
     */
    String HTTP_PROXY = "HttpProxy";

    /**
     * Claim of the token used in input of Data Plane public API containing the address of the
     * data source as an encrypted string.
     */
    String DATA_ADDRESS = "dad";

    /**
     * Claim of the token used in input of Data Plane public API containing the contract id.
     */
    String CONTRACT_ID = "cid";
}
