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

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.jwt.spi.JwtDecorator;

import java.util.Date;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;

/**
 * Decorator for access token used in input of Data Plane public API. The token is composed of:
 * - a contract id (used to check if contract between consumer and provider is still valid).
 * - the address of the data source formatted as an encrypted string.
 */
class ConsumerPullDataPlaneProxyTokenDecorator implements JwtDecorator {

    private final Date expirationDate;
    private final String encryptedDataAddress;

    ConsumerPullDataPlaneProxyTokenDecorator(Date expirationDate, String encryptedDataAddress) {
        this.expirationDate = expirationDate;
        this.encryptedDataAddress = encryptedDataAddress;
    }

    @Override
    public Map<String, Object> claims() {
        return Map.of(
                EXPIRATION_TIME, expirationDate,
                DATA_ADDRESS, encryptedDataAddress
        );
    }

    @Override
    public Map<String, Object> headers() {
        return emptyMap();
    }
}
