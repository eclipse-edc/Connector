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

package org.eclipse.edc.iam.identitytrust.sts.defaults;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.defaults.service.StsClientTokenGeneratorServiceImpl;
import org.eclipse.edc.iam.identitytrust.sts.defaults.store.InMemoryStsClientStore;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.VaultPrivateKeyResolver;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

import static com.nimbusds.jwt.JWTClaimNames.AUDIENCE;
import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.ISSUED_AT;
import static com.nimbusds.jwt.JWTClaimNames.ISSUER;
import static com.nimbusds.jwt.JWTClaimNames.JWT_ID;
import static com.nimbusds.jwt.JWTClaimNames.SUBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClientBuilder;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.CLIENT_ID;
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
                client -> new JwtGenerationService(keyId -> {
                    var pk = privateKeyResolver.resolvePrivateKey(keyId).orElse(null);
                    return CryptoConverter.createSignerFor(pk);
                }),
                StsClient::getPrivateKeyAlias,
                Clock.systemUTC(), 60 * 5);

    }

    @Test
    void authenticateAndGenerateToken() throws Exception {
        var id = "id";
        var clientId = "client_id";
        var secretAlias = "client_id";
        var privateKeyAlis = "client_id";
        var audience = "aud";
        var did = "did:example:subject";
        var client = createClientBuilder(id)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlis)
                .secretAlias(secretAlias)
                .publicKeyReference("public-key")
                .did(did)
                .build();

        var additional = StsClientTokenAdditionalParams.Builder.newInstance().audience(audience).build();

        vault.storeSecret(privateKeyAlis, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.create(client);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, did)
                .containsEntry(SUBJECT, did)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                .doesNotContainKey(CLIENT_ID);

    }

    @Test
    void authenticateAndGenerateToken_withBearerAccessScope() throws Exception {
        var id = "id";
        var clientId = "client_id";
        var secretAlias = "client_id";
        var privateKeyAlis = "client_id";
        var did = "did:example:subject";
        var audience = "aud";
        var scope = "scope:test";
        var client = createClientBuilder(id)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlis)
                .secretAlias(secretAlias)
                .did(did)
                .publicKeyReference("public-key")
                .build();

        var additional = StsClientTokenAdditionalParams.Builder.newInstance().audience(audience).bearerAccessScope(scope).build();

        vault.storeSecret(privateKeyAlis, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.create(client);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, did)
                .containsEntry(SUBJECT, did)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT, PRESENTATION_TOKEN_CLAIM)
                .doesNotContainKey(CLIENT_ID);

    }

    @Test
    void authenticateAndGenerateToken_withAccessToken() throws Exception {
        var id = "id";
        var clientId = "client_id";
        var secretAlias = "client_id";
        var privateKeyAlis = "client_id";
        var audience = "aud";
        var accessToken = "tokenTest";
        var did = "did:example:subject";

        var client = createClientBuilder(id)
                .clientId(clientId)
                .privateKeyAlias(privateKeyAlis)
                .secretAlias(secretAlias)
                .publicKeyReference("public-key")
                .did(did)
                .build();

        var additional = StsClientTokenAdditionalParams.Builder.newInstance().audience(audience).accessToken(accessToken).build();

        vault.storeSecret(privateKeyAlis, loadResourceFile("ec-privatekey.pem"));

        var createResult = clientService.create(client);
        assertThat(createResult.succeeded()).isTrue();

        var tokenResult = tokenGeneratorService.tokenFor(client, additional);
        var jwt = SignedJWT.parse(tokenResult.getContent().getToken());

        assertThat(jwt.getJWTClaimsSet().getClaims())
                .containsEntry(ISSUER, did)
                .containsEntry(SUBJECT, did)
                .containsEntry(AUDIENCE, List.of(audience))
                .containsEntry(PRESENTATION_TOKEN_CLAIM, accessToken)
                .containsKeys(JWT_ID, EXPIRATION_TIME, ISSUED_AT)
                .doesNotContainKey(CLIENT_ID);

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
