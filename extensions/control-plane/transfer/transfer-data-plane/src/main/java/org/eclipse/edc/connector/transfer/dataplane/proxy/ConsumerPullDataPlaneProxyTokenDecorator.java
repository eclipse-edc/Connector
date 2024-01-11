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

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

import java.util.Date;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;

/**
 * Decorator for access token used in input of Data Plane public API. The token is composed of:
 * - a contract id (used to check if contract between consumer and provider is still valid).
 * - the address of the data source formatted as an encrypted string.
 */
class ConsumerPullDataPlaneProxyTokenDecorator implements TokenDecorator {

    private final Date expirationDate;
    private final String encryptedDataAddress;

    ConsumerPullDataPlaneProxyTokenDecorator(Date expirationDate, String encryptedDataAddress) {
        this.expirationDate = expirationDate;
        this.encryptedDataAddress = encryptedDataAddress;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        return tokenParameters.claims(EXPIRATION_TIME, expirationDate)
                .claims(DATA_ADDRESS, encryptedDataAddress);
    }
}
