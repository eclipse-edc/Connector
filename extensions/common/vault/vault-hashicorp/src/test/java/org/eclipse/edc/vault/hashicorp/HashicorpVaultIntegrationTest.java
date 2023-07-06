/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *       Bayerische Motoren Werke Aktiengesellschaft - Refactoring
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.HealthStatus;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.vault.hashicorp.HashicorpVaultClient.VAULT_DATA_ENTRY_NAME;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_TOKEN;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_URL;
import static org.eclipse.edc.vault.hashicorp.util.X509CertificateTestUtil.convertToPem;
import static org.eclipse.edc.vault.hashicorp.util.X509CertificateTestUtil.generateCertificate;

@ComponentTest
@Testcontainers
@ExtendWith(EdcExtension.class)
class HashicorpVaultIntegrationTest {
    static final String DOCKER_IMAGE_NAME = "vault:1.9.6";
    static final String VAULT_ENTRY_KEY = "testing";
    static final String VAULT_ENTRY_VALUE = UUID.randomUUID().toString();
    static final String TOKEN = UUID.randomUUID().toString();

    @Container
    @ClassRule
    public static final VaultContainer<?> VAULTCONTAINER = new VaultContainer<>(DOCKER_IMAGE_NAME)
            .withVaultToken(TOKEN)
            .withSecretInVault("secret/" + VAULT_ENTRY_KEY, format("%s=%s", VAULT_DATA_ENTRY_NAME, VAULT_ENTRY_VALUE));

    private final TestExtension testExtension = new TestExtension();

    @BeforeEach
    final void beforeEach(EdcExtension extension) {
        extension.setConfiguration(getConfig());
        extension.registerServiceMock(HealthCheckService.class, new MyHealthCheckService());
        extension.registerSystemExtension(ServiceExtension.class, testExtension);
    }

    @Test
    @DisplayName("Resolve a secret that exists")
    void testResolveSecret_exists() {
        var vault = getVault();
        var secretValue = vault.resolveSecret(VAULT_ENTRY_KEY);
        assertThat(secretValue).isEqualTo(VAULT_ENTRY_VALUE);
    }

    @Test
    @DisplayName("Resolve a secret from a sub directory")
    void testResolveSecret_inSubDirectory() {
        var vault = getVault();
        var key = "sub/" + VAULT_ENTRY_KEY;
        var value = key + "value";

        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = { "foo!bar", "foo.bar", "foo[bar]", "sub/foo{bar}" })
    @DisplayName("Resolve a secret with url encoded characters")
    void testResolveSecret_withUrlEncodedCharacters(String key) {
        var vault = getVault();
        var value = key + "value";

        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value);
    }

    @Test
    @DisplayName("Resolve a secret that does not exist")
    void testResolveSecret_doesNotExist() {
        var vault = getVault();
        assertThat(vault.resolveSecret("wrong_key")).isNull();
    }

    @Test
    @DisplayName("Update a secret that exists")
    void testSetSecret_exists() {
        var key = UUID.randomUUID().toString();
        var value1 = UUID.randomUUID().toString();
        var value2 = UUID.randomUUID().toString();

        var vault = getVault();
        vault.storeSecret(key, value1);
        vault.storeSecret(key, value2);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value2);
    }

    @Test
    @DisplayName("Create a secret that does not exist")
    void testSetSecret_doesNotExist() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        var vault = getVault();
        vault.storeSecret(key, value);
        var secretValue = vault.resolveSecret(key);
        assertThat(secretValue).isEqualTo(value);
    }

    @Test
    @DisplayName("Delete a secret that exists")
    void testDeleteSecret_exists() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();

        var vault = getVault();
        vault.storeSecret(key, value);
        vault.deleteSecret(key);

        assertThat(vault.resolveSecret(key)).isNull();
    }

    @Test
    @DisplayName("Try to delete a secret that does not exist")
    void testDeleteSecret_doesNotExist() {
        var key = UUID.randomUUID().toString();

        var vault = getVault();
        vault.deleteSecret(key);

        assertThat(vault.resolveSecret(key)).isNull();
    }

    @Test
    void resolveCertificate_success() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException, org.bouncycastle.operator.OperatorCreationException {
        var key = UUID.randomUUID().toString();
        var certificateExpected = generateCertificate(5, "Test");
        var pem = convertToPem(certificateExpected);

        var vault = getVault();
        vault.storeSecret(key, pem);
        var resolver = getCertificateResolver();
        var certificateResult = resolver.resolveCertificate(key);

        assertThat(certificateExpected).isEqualTo(certificateResult);
    }

    @Test
    void resolveCertificate_malformed() {
        var key = UUID.randomUUID().toString();
        var value = UUID.randomUUID().toString();
        var vault = getVault();
        vault.storeSecret(key, value);

        var resolver = getCertificateResolver();
        var certificateResult = resolver.resolveCertificate(key);
        assertThat(certificateResult).isNull();
    }

    protected Vault getVault() {
        return testExtension.getVault();
    }

    protected CertificateResolver getCertificateResolver() {
        return testExtension.getCertificateResolver();
    }

    protected Map<String, String> getConfig() {
        return new HashMap<>() {
            {
                put(VAULT_URL, format("http://%s:%s", VAULTCONTAINER.getHost(), VAULTCONTAINER.getFirstMappedPort()));
                put(VAULT_TOKEN, TOKEN);
            }
        };
    }

    private static class TestExtension implements ServiceExtension {
        private Vault vault;
        private CertificateResolver certificateResolver;

        @Override
        public void initialize(ServiceExtensionContext context) {
            vault = context.getService(Vault.class);
            certificateResolver = context.getService(CertificateResolver.class);
        }

        public CertificateResolver getCertificateResolver() {
            return certificateResolver;
        }

        public Vault getVault() {
            return vault;
        }
    }

    private static class MyHealthCheckService implements HealthCheckService {
        private final List<LivenessProvider> livenessProviders = new ArrayList<>();
        private final List<ReadinessProvider> readinessProviders = new ArrayList<>();
        private final List<StartupStatusProvider> startupStatusProviders = new ArrayList<>();

        @Override
        public void addLivenessProvider(LivenessProvider provider) {
            livenessProviders.add(provider);
        }

        @Override
        public void addReadinessProvider(ReadinessProvider provider) {
            readinessProviders.add(provider);
        }

        @Override
        public void addStartupStatusProvider(StartupStatusProvider provider) {
            startupStatusProviders.add(provider);
        }

        @Override
        public HealthStatus isLive() {
            return new HealthStatus(
                    livenessProviders.stream()
                            .map(
                                    p ->
                                            p.get().failed() ? HealthCheckResult.failed("") : HealthCheckResult.success())
                            .collect(Collectors.toList()));
        }

        @Override
        public HealthStatus isReady() {
            return new HealthStatus(
                    readinessProviders.stream()
                            .map(
                                    p ->
                                            p.get().failed() ? HealthCheckResult.failed("") : HealthCheckResult.success())
                            .collect(Collectors.toList()));
        }

        @Override
        public HealthStatus getStartupStatus() {
            return new HealthStatus(
                    startupStatusProviders.stream()
                            .map(
                                    p ->
                                            p.get().failed() ? HealthCheckResult.failed("") : HealthCheckResult.success())
                            .collect(Collectors.toList()));
        }

        @Override
        public void refresh() {
        }
    }
}
