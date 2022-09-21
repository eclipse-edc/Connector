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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy;

import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

import java.util.Date;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.CONTRACT_ID;
import static org.eclipse.dataspaceconnector.transfer.dataplane.spi.DataPlaneTransferConstants.DATA_ADDRESS;

/**
 * Decorator for access token used in input of Data Plane public API. The token is composed of:
 * - a contract id (used to check if contract between consumer and provider is still valid).
 * - the address of the data source formatted as an encrypted string.
 */
public class DataPlaneProxyTokenDecorator implements JwtDecorator {

    private final Date expirationDate;
    private final String contractId;
    private final String encryptedDataAddress;

    public DataPlaneProxyTokenDecorator(Date expirationDate, String contractId, String encryptedDataAddress) {
        this.expirationDate = expirationDate;
        this.contractId = contractId;
        this.encryptedDataAddress = encryptedDataAddress;
    }

    @Override
    public Map<String, Object> claims() {
        return Map.of(
                EXPIRATION_TIME, expirationDate,
                CONTRACT_ID, contractId,
                DATA_ADDRESS, encryptedDataAddress
        );
    }

    @Override
    public Map<String, Object> headers() {
        return emptyMap();
    }
}
