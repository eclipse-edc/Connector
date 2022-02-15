/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.daps;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.core.security.fs.FsCertificateResolver;
import org.eclipse.dataspaceconnector.core.security.fs.FsPrivateKeyResolver;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.junit.launcher.MockVault;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ExtendWith(EdcExtension.class)
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

    @BeforeEach
    protected void before(EdcExtension extension) {
        KeyStore clientKeystore = readKeystoreFromResources("keystore.p12", "PKCS12", CLIENT_KEYSTORE_PASSWORD);
        extension.registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) () -> ConfigFactory.fromMap(configuration));
        extension.registerServiceMock(Vault.class, new MockVault());
        extension.registerServiceMock(PrivateKeyResolver.class, new FsPrivateKeyResolver(CLIENT_KEYSTORE_PASSWORD, clientKeystore));
        extension.registerServiceMock(CertificateResolver.class, new FsCertificateResolver(clientKeystore));
    }

    private static KeyStore readKeystoreFromResources(String fileName, String type, String password) {
        var url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        Objects.requireNonNull(url);

        try {
            var ks = KeyStore.getInstance(type);
            var fis = new FileInputStream(url.getFile());
            ks.load(fis, password.toCharArray());
            return ks;
        } catch (Exception e) {
            throw new EdcException("Failed to load keystore: " + e);
        }
    }

    @Test
    void retrieveTokenAndValidate(IdentityService identityService) {
        var tokenResult = identityService.obtainClientCredentials("idsc:IDS_CONNECTOR_ATTRIBUTES_ALL");

        assertThat(tokenResult.succeeded()).isTrue();

        var verificationResult = identityService.verifyJwtToken(tokenResult.getContent().getToken());

        assertThat(verificationResult.succeeded()).isTrue();
    }

}
