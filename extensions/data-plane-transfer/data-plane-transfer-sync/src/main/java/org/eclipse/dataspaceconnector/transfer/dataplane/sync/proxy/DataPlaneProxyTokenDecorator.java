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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.JwtDecorator;

import java.util.Date;

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
    public void decorate(JWSHeader.Builder header, JWTClaimsSet.Builder claimsSet) {
        claimsSet.expirationTime(expirationDate)
                .claim(CONTRACT_ID, contractId)
                .claim(DATA_ADDRESS, encryptedDataAddress)
                .build();
    }
}
