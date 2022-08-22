/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.iam.daps;

import org.eclipse.dataspaceconnector.iam.daps.annotations.DapsTest;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
@DapsTest
class DapsIntegrationTest {

    private static final String AUDIENCE_IDS_CONNECTORS_ALL = "idsc:IDS_CONNECTORS_ALL";
    private static final String CLIENT_ID = "68:99:2E:D4:13:2D:FD:3A:66:6B:85:DE:FB:98:2E:2D:FD:E7:83:D7";
    private static final String CLIENT_KEYSTORE_KEY_ALIAS = "1";
    private static final String CLIENT_KEYSTORE_PASSWORD = "1234";
    private static final String DAPS_URL = "http://localhost:4567";

    private final Map<String, String> configuration = Map.of(
            "edc.oauth.token.url", DAPS_URL + "/token",
            "edc.oauth.client.id", CLIENT_ID,
            "edc.oauth.provider.audience", AUDIENCE_IDS_CONNECTORS_ALL,
            "edc.oauth.provider.jwks.url", DAPS_URL + "/.well-known/jwks.json",
            "edc.oauth.public.key.alias", CLIENT_KEYSTORE_KEY_ALIAS,
            "edc.oauth.private.key.alias", CLIENT_KEYSTORE_KEY_ALIAS
    );

    @Test
    void retrieveTokenAndValidate(IdentityService identityService) {
        var tokenParameters = TokenParameters.Builder.newInstance()
                .scope("idsc:IDS_CONNECTOR_ATTRIBUTES_ALL")
                .audience("audience")
                .build();
        var tokenResult = identityService.obtainClientCredentials(tokenParameters);

        assertThat(tokenResult.succeeded()).isTrue();

        var verificationResult = identityService.verifyJwtToken(tokenResult.getContent(), "audience");

        assertThat(verificationResult.succeeded()).isTrue();
    }

    @BeforeEach
    protected void before(EdcExtension extension) {
        System.setProperty("edc.vault", "src/test/resources/empty-vault.properties");
        System.setProperty("edc.keystore", "src/test/resources/keystore.p12");
        System.setProperty("edc.keystore.password", CLIENT_KEYSTORE_PASSWORD);
        extension.setConfiguration(configuration);
    }

}
