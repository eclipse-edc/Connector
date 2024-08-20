/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.iam.service;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.token.spi.TokenDecorator;

import static org.eclipse.edc.connector.dataplane.iam.service.DefaultDataPlaneAccessTokenServiceImpl.TOKEN_ID;

public class TokenIdDecorator implements TokenDecorator {
    private final String tokenId;

    public TokenIdDecorator(String tokenId) {
        this.tokenId = tokenId;
    }

    @Override
    public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
        tokenParameters.claims(TOKEN_ID, tokenId);
        return tokenParameters;
    }
}
