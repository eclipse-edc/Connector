/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.vault.VaultContainer;

import java.util.Map;

/**
 * Extension to be used in end-to-end tests with Vault
 */
public class VaultEndToEndExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    static final String DOCKER_IMAGE_NAME = "hashicorp/vault:1.18.3";
    static final String TOKEN = "root";

    private final VaultContainer<?> vault;

    private final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient();
    private ObjectMapper objectMapper = new ObjectMapper();

    private VaultApi vaultApi;

    public VaultEndToEndExtension() {
        this(DOCKER_IMAGE_NAME);
    }

    @SuppressWarnings("resource")
    public VaultEndToEndExtension(String dockerImageName) {
        this(new VaultContainer<>(dockerImageName)
                .withVaultToken(TOKEN)
                .withExposedPorts(8200));
    }

    public VaultEndToEndExtension(VaultContainer<?> container) {
        vault = container;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        vault.start();
    }

    public String getVaultUrl() {
        return "http://" + vault.getHost() + ":" + vault.getMappedPort(8200);
    }

    public Config configFor() {
        Map<String, String> settings = Map.of(
                "edc.vault.hashicorp.url", getVaultUrl(),
                "edc.vault.hashicorp.token", TOKEN
        );
        return ConfigFactory.fromMap(settings);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        vault.stop();
        vault.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(VaultApi.class);

    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(VaultApi.class)) {
            return getVaultApi();
        }
        return null;
    }

    public VaultApi getVaultApi() {
        if (vaultApi == null) {
            vaultApi = new VaultApi(getVaultUrl(), TOKEN);
        }
        return vaultApi;
    }

}
