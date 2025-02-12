/*
 *  Copyright (c) 2024 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


class HashicorpVaultTokenRenewServiceIntegrationTest {

    @ComponentTest
    @Testcontainers
    @Nested
    abstract static class Tests {

        protected static final String HTTP_URL_FORMAT = "http://%s:%s";
        protected static final String HEALTH_CHECK_PATH = "/health/path";
        protected static final String CLIENT_TOKEN_KEY = "client_token";
        protected static final String AUTH_KEY = "auth";
        protected static final long CREATION_TTL = 6L;
        protected static final long TTL = 5L;
        protected static final long RENEW_BUFFER = 4L;
        protected HashicorpVaultTokenRenewService tokenRenewService;
        protected final ObjectMapper mapper = new ObjectMapper();
        protected final ConsoleMonitor monitor = new ConsoleMonitor();

        @BeforeEach
        void beforeEach() throws IOException, InterruptedException {
            Assertions.assertThat(CREATION_TTL).isGreaterThan(TTL);
        }

        @Test
        void lookUpToken_whenTokenNotExpired_shouldSucceed() {
            var tokenLookUpResult = tokenRenewService.isTokenRenewable();

            assertThat(tokenLookUpResult).isSucceeded().isEqualTo(true);
        }

        @Test
        void lookUpToken_whenTokenExpired_shouldFail() {
            await()
                    .pollDelay(CREATION_TTL, TimeUnit.SECONDS)
                    .atMost(CREATION_TTL + 1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        var tokenLookUpResult = tokenRenewService.isTokenRenewable();
                        assertThat(tokenLookUpResult).isFailed();
                        Assertions.assertThat(tokenLookUpResult.getFailureDetail()).isEqualTo("Token look up failed with status 403");
                    });
        }

        @Test
        void renewToken_whenTokenNotExpired_shouldSucceed() {
            var tokenRenewResult = tokenRenewService.renewToken();

            assertThat(tokenRenewResult).isSucceeded().satisfies(ttl -> Assertions.assertThat(ttl).isEqualTo(TTL));
        }

        @Test
        void renewToken_whenTokenExpired_shouldFail() {
            await()
                    .pollDelay(CREATION_TTL, TimeUnit.SECONDS)
                    .atMost(CREATION_TTL + 1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        var tokenRenewResult = tokenRenewService.renewToken();
                        assertThat(tokenRenewResult).isFailed();
                        Assertions.assertThat(tokenRenewResult.getFailureDetail()).isEqualTo("Token renew failed with status: 403");
                    });
        }
    }

    @ComponentTest
    @Testcontainers
    @Nested
    class LastKnownFoss extends Tests {
        @Container
        static final VaultContainer<?> VAULT_CONTAINER = new VaultContainer<>("vault:1.9.6")
                .withVaultToken(UUID.randomUUID().toString());

        public static HashicorpVaultSettings getSettings() throws IOException, InterruptedException {
            var execResult = VAULT_CONTAINER.execInContainer(
                    "vault",
                    "token",
                    "create",
                    "-policy=root",
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

            return HashicorpVaultSettings.Builder.newInstance()
                    .url(HTTP_URL_FORMAT.formatted(VAULT_CONTAINER.getHost(), VAULT_CONTAINER.getFirstMappedPort()))
                    .healthCheckPath(HEALTH_CHECK_PATH)
                    .token(clientToken)
                    .ttl(TTL)
                    .renewBuffer(RENEW_BUFFER)
                    .build();
        }

        @BeforeEach
        void beforeEach() throws IOException, InterruptedException {
            HashicorpVaultSettings settings = getSettings();
            tokenRenewService = new HashicorpVaultTokenRenewService(
                    testHttpClient(),
                    mapper,
                    settings,
                    new HashicorpVaultTokenProviderImpl(settings.token()),
                    monitor
            );
        }
    }

    @ComponentTest
    @Testcontainers
    @Nested
    class Latest extends Tests {
        @Container
        static final VaultContainer<?> VAULT_CONTAINER = new VaultContainer<>("hashicorp/vault:1.18.3")
                .withVaultToken(UUID.randomUUID().toString());

        public static HashicorpVaultSettings getSettings() throws IOException, InterruptedException {
            var execResult = VAULT_CONTAINER.execInContainer(
                    "vault",
                    "token",
                    "create",
                    "-policy=root",
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

            return HashicorpVaultSettings.Builder.newInstance()
                    .url(HTTP_URL_FORMAT.formatted(VAULT_CONTAINER.getHost(), VAULT_CONTAINER.getFirstMappedPort()))
                    .healthCheckPath(HEALTH_CHECK_PATH)
                    .token(clientToken)
                    .ttl(TTL)
                    .renewBuffer(RENEW_BUFFER)
                    .build();
        }

        @BeforeEach
        void beforeEach() throws IOException, InterruptedException {
            HashicorpVaultSettings settings = getSettings();
            tokenRenewService = new HashicorpVaultTokenRenewService(
                    testHttpClient(),
                    mapper,
                    settings,
                    new HashicorpVaultTokenProviderImpl(settings.token()),
                    monitor
            );
        }
    }
}
