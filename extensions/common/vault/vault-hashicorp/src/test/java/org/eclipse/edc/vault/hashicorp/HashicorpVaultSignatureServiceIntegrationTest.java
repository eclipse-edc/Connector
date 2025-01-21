/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings.VAULT_API_HEALTH_PATH_DEFAULT;
import static org.mockito.Mockito.mock;

@ComponentTest
@Testcontainers
class HashicorpVaultSignatureServiceIntegrationTest {
    static final String DOCKER_IMAGE_NAME = "vault:1.9.6";
    static final String TOKEN = UUID.randomUUID().toString();
    @Container
    private static final VaultContainer<?> VAULTCONTAINER = new VaultContainer<>(DOCKER_IMAGE_NAME)
            .withVaultToken(TOKEN);
    private static Monitor monitor;
    private static HashicorpVaultSettings settings;
    private static EdcHttpClientImpl httpClient;
    private final byte[] testPayload = "test signing input // *    ".getBytes();
    private String vaultKey;
    private HashicorpVaultSignatureService service;

    private static Integer getPort() {
        // container might not be started, lazily start and wait for it to come up
        if (!VAULTCONTAINER.isRunning()) {
            VAULTCONTAINER.start();
            VAULTCONTAINER.waitingFor(Wait.forHealthcheck());
        }
        return VAULTCONTAINER.getFirstMappedPort();
    }

    @BeforeAll
    static void prepare() throws IOException {

        monitor = mock(Monitor.class);
        settings = org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings.Builder.newInstance()
                .url("http://localhost:%s".formatted(getPort()))
                .healthCheckPath(VAULT_API_HEALTH_PATH_DEFAULT)
                .ttl(24 * 60)
                .token(TOKEN)
                .build();
        httpClient = new EdcHttpClientImpl(new OkHttpClient.Builder().build(), RetryPolicy.ofDefaults(), monitor);


        // activate transit secrets engine
        var payload = """
                {"type":"transit"}
                """;
        var rq = new Request.Builder()
                .url("http://localhost:%s/v1/sys/mounts/transit".formatted(getPort()))
                .header("X-Vault-Token", TOKEN)
                .post(RequestBody.create(payload.getBytes(), MediaType.parse("application/json")))
                .build();
        try (var response = httpClient.execute(rq)) {
            assertThat(response.isSuccessful()).describedAs(response.message()).isTrue();
        }
    }

    @BeforeEach
    void setUp() throws IOException {

        vaultKey = UUID.randomUUID().toString();
        service = new HashicorpVaultSignatureService(monitor, settings, httpClient, new ObjectMapper());


        // create a new testing key
        var payload2 = """
                {"type":"ecdsa-p256"}
                """;
        var rq2 = new Request.Builder()
                .url("http://localhost:%s/v1/transit/keys/%s".formatted(getPort(), vaultKey))
                .header("X-Vault-Token", TOKEN)
                .post(RequestBody.create(payload2.getBytes(), MediaType.parse("application/json")))
                .build();
        try (var response = httpClient.execute(rq2)) {
            assertThat(response.isSuccessful()).describedAs(response.message()).isTrue();
        }
    }

    @Test
    void sign() {
        var signature = service.sign(vaultKey, testPayload, "");
        assertThat(signature).withFailMessage(signature::getFailureDetail)
                .isSucceeded()
                .extracting(String::new)
                .matches(sig -> sig.startsWith("vault:v1"));
    }

    @Test
    void sign_keyNotExist() {
        assertThat(service.sign("notexist", testPayload, "")).isFailed()
                .detail().isEqualTo("Failed to sign payload with status 400, Bad Request");

    }

    @Test
    void verify() {
        var signature = new String(service.sign(vaultKey, testPayload, "").getContent());

        assertThat(service.verify(vaultKey, testPayload, signature.getBytes(), null))
                .isSucceeded();

    }

    @Test
    void verify_keyNotExist() {
        var signature = new String(service.sign(vaultKey, testPayload, "").getContent());

        assertThat(service.verify("not-exist", testPayload, signature.getBytes(), null))
                .isFailed()
                .detail().isEqualTo("Failed to verify signature with status 400, Bad Request");

    }

    @Test
    void verify_invalidSignature() {
        assertThat(service.verify(vaultKey, testPayload, "vault:v1:invalid-signature".getBytes(), null))
                .isFailed()
                .detail().isEqualTo("Failed to verify signature with status 400, Bad Request");

    }

    @Test
    void rotate() {
    }
}
