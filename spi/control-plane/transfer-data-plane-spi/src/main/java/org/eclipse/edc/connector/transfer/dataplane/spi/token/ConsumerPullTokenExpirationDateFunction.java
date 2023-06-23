/*
 *  Copyright (c) 2023 Amadeus
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

package org.eclipse.edc.connector.transfer.dataplane.spi.token;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Date;

@FunctionalInterface
public interface ConsumerPullTokenExpirationDateFunction {

    /**
     * Determines expieration date of token used to query data through Data Plane.
     *
     * @param contentAddress source data address.
     * @param contractId     contract id.
     * @return Token expiration date.
     */
    Result<Date> expiresAt(DataAddress contentAddress, String contractId);
}
