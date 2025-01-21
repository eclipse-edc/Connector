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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.SignatureService;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultHealthService;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultTokenRenewTask;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Extension(value = HashicorpVaultExtension.NAME)
public class HashicorpVaultExtension implements ServiceExtension {
    public static final String NAME = "Hashicorp Vault";
    public static final ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Configuration
    private HashicorpVaultSettings config;

    private HashicorpVaultTokenRenewTask tokenRenewalTask;
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault hashicorpVault() {
        return new HashicorpVault(monitor, config, httpClient, MAPPER);
    }

    @Provider
    public SignatureService signatureService() {
        return new HashicorpVaultSignatureService(monitor, config, httpClient, MAPPER);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix(NAME);
        tokenRenewalTask = new HashicorpVaultTokenRenewTask(
                NAME,
                executorInstrumentation,
                new HashicorpVaultHealthService(httpClient, MAPPER, monitor, config),
                config.renewBuffer(),
                monitor);
    }

    @Override
    public void start() {
        if (config.scheduledTokenRenewEnabled()) {
            tokenRenewalTask.start();
        }
    }

    @Override
    public void shutdown() {
        if (tokenRenewalTask.isRunning()) {
            tokenRenewalTask.stop();
        }
    }
}
