/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp;

import jakarta.json.Json;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_RETRY_BACKOFF_BASE;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_RENEW_BUFFER;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_TOKEN_TTL;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultConfig.VAULT_URL;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ComponentTest
@Testcontainers
@ExtendWith(EdcExtension.class)
class HashicorpVaultClientIntegrationTest {
    private static final String DOCKER_IMAGE_NAME = "vault:1.9.6";
    private static final String VAULT_ENTRY_KEY = "testing";
    private static final String VAULT_ENTRY_VALUE = UUID.randomUUID().toString();
    private static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String ROOT_TOKEN = UUID.randomUUID().toString();
    private static final double RETRY_BACKOFF_BASE = 1.1;
    private static final long CREATION_TTL = 6L;
    private static final long TTL = 5L;
    private static final long RENEW_BUFFER = 4;
    private static final String CLIENT_TOKEN_KEY = "client_token";
    private static final String AUTH_KEY = "auth";

    @Container
    static final VaultContainer<?> VAULTCONTAINER = new VaultContainer<>(DOCKER_IMAGE_NAME)
            .withVaultToken(ROOT_TOKEN)
            .withSecretInVault("secret/" + VAULT_ENTRY_KEY, format("%s=%s", VAULT_DATA_ENTRY_NAME, VAULT_ENTRY_VALUE));

    @BeforeEach
    void beforeEach(EdcExtension extension) throws IOException, InterruptedException {
        assertThat(CREATION_TTL).isNotEqualTo(TTL);
        extension.setConfiguration(getConfig());
    }

    @Test
    void lookUpToken_whenTokenTtlNotExpired_shouldSucceed(HashicorpVaultClient hashicorpVaultClient) {
        var tokenLookUpResult = hashicorpVaultClient.lookUpToken(CREATION_TTL);

        assertThat(tokenLookUpResult.succeeded()).isTrue();
        var tokenLookUpResponse = tokenLookUpResult.getContent();
        assertThat(tokenLookUpResponse).isNotNull();
        assertThat(tokenLookUpResponse.isRenewable()).isFalse();
        assertThat(tokenLookUpResponse.getWarnings()).isEmpty();
        assertThat(tokenLookUpResponse.getLeaseDuration()).isEqualTo(0L);
        assertThat(tokenLookUpResponse.getRequestId()).isNotNull();
        assertThat(tokenLookUpResponse.getLeaseId()).isNotNull();
        var tokenLookUpData = tokenLookUpResponse.getData();
        assertThat(tokenLookUpData.getCreationTime()).isGreaterThan(0);
        assertThat(tokenLookUpData.getCreationTtl()).isEqualTo(CREATION_TTL);
        assertThat(tokenLookUpData.getAccessor()).isNotEmpty();
        assertThat(tokenLookUpData.getPolicies()).isEqualTo(List.of("default"));
        assertThat(tokenLookUpData.getExpireTime()).isNotEmpty();
        assertThat(tokenLookUpData.getNumUses()).isEqualTo(0);
        assertThat(tokenLookUpData.getDisplayName()).isEqualTo("token");
        assertThat(tokenLookUpData.getEntityId()).isNotNull();
        assertThat(tokenLookUpData.isOrphan()).isFalse();
        assertThat(tokenLookUpData.getType()).isEqualTo("service");
        assertThat(tokenLookUpData.getTtl()).isLessThan(CREATION_TTL);
        assertThat(tokenLookUpData.getExplicitMaxTtl()).isEqualTo(0L);
        assertThat(tokenLookUpData.getPath()).isNotNull();
        assertThat(tokenLookUpData.getPeriod()).isNull();
        assertThat(tokenLookUpData.getMeta()).isEmpty();
        assertThat(tokenLookUpData.isRenewable()).isTrue();
        assertThat(tokenLookUpData.getId()).isNotNull();
        assertThat(tokenLookUpData.getIssueTime()).isNotNull();
        assertThat(tokenLookUpData.getPeriod()).isNull();
    }

    @Test
    void lookUpToken_whenTokenTtlExpired_shouldFail(HashicorpVaultClient hashicorpVaultClient) {
        await()
                .pollDelay(CREATION_TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var tokenLookUpResult = hashicorpVaultClient.lookUpToken(CREATION_TTL);
                    assertThat(tokenLookUpResult.failed()).isTrue();
                    assertThat(tokenLookUpResult.getFailureDetail()).isEqualTo("Token look up failed with status 403");
                });
    }

    @Test
    void renewToken_whenTokenTtlNotExpired_shouldSucceed(HashicorpVaultClient hashicorpVaultClient) {
        var tokenRenewResult = hashicorpVaultClient.renewToken(TTL);

        assertThat(tokenRenewResult.succeeded()).isTrue();
        var tokenRenewResponse = tokenRenewResult.getContent();
        assertThat(tokenRenewResponse).isNotNull();
        assertThat(tokenRenewResponse.getRequestId()).isNotNull();
        assertThat(tokenRenewResponse.isRenewable()).isEqualTo(false);
        assertThat(tokenRenewResponse.getWarnings()).isEmpty();
        assertThat(tokenRenewResponse.getLeaseDuration()).isEqualTo(0L);
        assertThat(tokenRenewResponse.getLeaseId()).isNotNull();
        var tokenRenewAuth = tokenRenewResponse.getAuth();
        assertThat(tokenRenewAuth.getTokenPolicies()).isEqualTo(List.of("default"));
        assertThat(tokenRenewAuth.getClientToken()).isNotEmpty();
        assertThat(tokenRenewAuth.getMetadata()).isEmpty();
        assertThat(tokenRenewAuth.isRenewable()).isTrue();
        assertThat(tokenRenewAuth.getAccessor()).isNotEmpty();
        assertThat(tokenRenewAuth.getPolicies()).isEqualTo(List.of("default"));
        assertThat(tokenRenewAuth.getLeaseDuration()).isEqualTo(TTL);
        assertThat(tokenRenewAuth.isOrphan()).isFalse();
        assertThat(tokenRenewAuth.getEntityId()).isEmpty();
    }

    @Test
    void renew_whenTokenTtlExpired_shouldFail(HashicorpVaultClient hashicorpVaultClient) {
        await()
                .pollDelay(CREATION_TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var tokenRenewResult = hashicorpVaultClient.renewToken(TTL);
                    assertThat(tokenRenewResult.failed()).isTrue();
                    assertThat(tokenRenewResult.getFailureDetail()).isEqualTo("Token renew failed with status: 403");
                });
    }

    @Test
    void scheduleTokenRenewal_withValidToken_shouldSucceed(HashicorpVaultClient hashicorpVaultClient) {
        // trigger the automatic token renewal mechanism in the background
        hashicorpVaultClient.scheduleTokenRenewal();

        // ensure that the token is still valid after the initial creation_ttl expired
        await()
                .pollDelay(CREATION_TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var tokenLookUpResult = hashicorpVaultClient.lookUpToken(CREATION_TTL);
                    assertThat(tokenLookUpResult.succeeded()).isTrue();
                });

        // at this point the creation ttl should be overridden by the renewal operation
        // check that the token is still valid after the new ttl expired
        await()
                .pollDelay(TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var tokenLookUpResult = hashicorpVaultClient.lookUpToken(TTL);
                    assertThat(tokenLookUpResult.succeeded()).isTrue();
                });
    }

    private Map<String, String> getConfig() throws IOException, InterruptedException {
        var execResult = VAULTCONTAINER.execInContainer(
                "vault",
                "token",
                "create",
                "-policy=default",
                "-ttl=%d".formatted(CREATION_TTL),
                "-format=json");

        var jsonParser = Json.createParser(new StringReader(execResult.getStdout()));
        jsonParser.next();
        var auth = jsonParser.getObjectStream().filter(e -> e.getKey().equals(AUTH_KEY))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow()
                .asJsonObject();
        var clientToken = auth.getString(CLIENT_TOKEN_KEY);

        return new HashMap<>() {
            {
                put(VAULT_URL, format("http://%s:%s", VAULTCONTAINER.getHost(), VAULTCONTAINER.getFirstMappedPort()));
                put(VAULT_TOKEN, clientToken);
                put(VAULT_TOKEN_SCHEDULED_RENEWAL_ENABLED, Boolean.toString(false));
                put(VAULT_RETRY_BACKOFF_BASE, Double.toString(RETRY_BACKOFF_BASE));
                put(VAULT_TOKEN_TTL, Long.toString(TTL));
                put(VAULT_TOKEN_RENEW_BUFFER, Long.toString(RENEW_BUFFER));
            }
        };
    }
}
