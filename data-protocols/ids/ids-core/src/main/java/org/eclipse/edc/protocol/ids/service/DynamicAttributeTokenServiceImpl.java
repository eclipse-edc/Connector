/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.ids.service;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;
import java.util.Map;

public class DynamicAttributeTokenServiceImpl implements DynamicAttributeTokenService {

    private static final String TOKEN_SCOPE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";

    private IdentityService identityService;

    public DynamicAttributeTokenServiceImpl(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public Result<DynamicAttributeToken> obtainDynamicAttributeToken(String recipientAddress) {
        var tokenParameters = TokenParameters.Builder.newInstance()
                .scope(TOKEN_SCOPE)
                .audience(recipientAddress)
                .build();
        return identityService.obtainClientCredentials(tokenParameters)
                .map(credentials -> new DynamicAttributeTokenBuilder()
                        ._tokenFormat_(TokenFormat.JWT)
                        ._tokenValue_(credentials.getToken())
                        .build()
                );
    }

    @Override
    public Result<ClaimToken> verifyDynamicAttributeToken(DynamicAttributeToken token, URI issuerConnector, String audience) {
        // Prepare DAT validation: IDS token validation requires issuerConnector
        var additional = Map.<String, Object>of("issuerConnector", issuerConnector);

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token.getTokenValue())
                .additional(additional)
                .build();

        return identityService.verifyJwtToken(tokenRepresentation, audience);
    }
}
