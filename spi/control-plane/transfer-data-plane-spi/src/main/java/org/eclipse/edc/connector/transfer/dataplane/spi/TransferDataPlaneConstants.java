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

package org.eclipse.edc.connector.transfer.dataplane.spi;

import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * Type of Data Plane transfer.
 */
public interface TransferDataPlaneConstants {
    /**
     * {@link DataFlowStartMessage} type that triggers client-pull data transfer.
     */
    String HTTP_PROXY = "HttpProxy";

    /**
     * Claim of the token used in input of Data Plane public API containing the address of the
     * data source as an encrypted string.
     */
    String DATA_ADDRESS = "dad";
}
