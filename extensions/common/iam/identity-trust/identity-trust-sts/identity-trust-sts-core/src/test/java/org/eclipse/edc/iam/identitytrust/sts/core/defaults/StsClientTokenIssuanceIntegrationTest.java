/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.core.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.core.security.KeyParserRegistryImpl;
import org.eclipse.edc.connector.core.security.keyparsers.JwkParser;
import org.eclipse.edc.connector.core.security.keyparsers.PemParser;
import org.eclipse.edc.connector.core.vault.InMemoryVault;
import org.eclipse.edc.iam.identitytrust.sts.core.defaults.service.StsClientServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.core.defaults.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.core.defaults.store.InMemoryStsClientStore;
import org.eclipse.edc.iam.identitytrust.sts.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.jwt.JwtGenerationService;
import org.eclipse.edc.jwt.spi.SignatureInfo;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.store.fixtures.TestFunctions.createClientBuilder;
import static org.eclipse.edc.identitytrust.SelfIssuedTokenConstants.PRESENTATION_ACCESS_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.mockito.Mockito.mock;

@ComponentTest
public class StsClientTokenIssuanceIntegrationTest {

    private final InMemoryStsClientStore clientStore = new InMemoryStsClientStore();
    private final Vault vault = new InMemoryVault(mock());
    private final KeyParserRegistry keyParserRegistry = new KeyParserRegistryImpl();
    private StsClientServiceImpl clientService;
    private StsClientTokenGeneratorServiceImpl tokenGeneratorService;
    private PrivateKeyResolver privateKeyResolver;

    @BeforeEach
    void setup() {
        clientService = new StsClientServiceImpl(clientStore, vault, new NoopTransactionContext());

        keyParserRegistry.register(new PemParser(mock()));
        keyParserRegistry.register(new JwkParser(new ObjectMapper(), mock()));
        privateKeyResolver = new VaultPrivateKeyResolver(keyParserRegistry, vault, mock(), mock());

        tokenGeneratorService = new StsClientTokenGeneratorServiceImpl(
                client -> new JwtGenerationService(),
                stsClient -> new SignatureInfo(privateKeyResolver.resolvePrivateKey(stsClient.getPrivateKeyAlias()).orElse(null), stsClient.getPublicKeyReference()),
                Clock.systemUTC(), 60 * 5);

    }

    @Test
    void authenticateAndGenerateToken() throws Exception {
        var id = "id";
        var clientId = "client_id";
        var secretAlias = "client_id";
        var privateKeyAlis = "client_id";
        var audience = "aud";
        var client = createClientBuilder(id)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlis)
                .secretAlias(secretAlias)
                .build();

        var additional = StsClientTokenAdditionalParams.Builder.newInstance().audience(audience).build();

        vault.storeSecret(privateKeyAlis, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.create(client);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, id)
                .containsEntry(SUBJECT, id)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsEntry(CLIENT_ID, clientId)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);

    }

    @Test
    void authenticateAndGenerateToken_withBearerAccessScope() throws Exception {
        var id = "id";
        var clientId = "client_id";
        var secretAlias = "client_id";
        var privateKeyAlis = "client_id";
        var audience = "aud";
        var scope = "scope:test";
        var client = createClientBuilder(id)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlis)
                .secretAlias(secretAlias)
                .build();

        var additional = StsClientTokenAdditionalParams.Builder.newInstance().audience(audience).bearerAccessScope(scope).build();

        vault.storeSecret(privateKeyAlis, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.create(client);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, id)
                .containsEntry(SUBJECT, id)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsEntry(CLIENT_ID, clientId)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT, "access_token");

    }

    @Test
    void authenticateAndGenerateToken_withAccessToken() throws Exception {
        var id = "id";
        var clientId = "client_id";
        var secretAlias = "client_id";
        var privateKeyAlis = "client_id";
        var audience = "aud";
        var accessToken = "tokenTest";
        var client = createClientBuilder(id)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlis)
                .secretAlias(secretAlias)
                .build();

        var additional = StsClientTokenAdditionalParams.Builder.newInstance().audience(audience).accessToken(accessToken).build();

        vault.storeSecret(privateKeyAlis, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.create(client);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, id)
                .containsEntry(SUBJECT, id)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsEntry(CLIENT_ID, clientId)
                .containsEntry(PRESENTATION_ACCESS_TOKEN_CLAIM, accessToken)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT);

    }

    /**
     * Load content from a resource file.
     */
    private String loadResourceFile(String file) throws IOException {
        try (var resourceAsStream = StsClientTokenIssuanceIntegrationTest.class.getClassLoader().getResourceAsStream(file)) {
            return new String(Objects.requireNonNull(resourceAsStream).readAllBytes());
        }
    }
}
