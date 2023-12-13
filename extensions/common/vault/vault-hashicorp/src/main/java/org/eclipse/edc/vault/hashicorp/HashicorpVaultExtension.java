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
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Provides({Vault.class, PrivateKeyResolver.class, CertificateResolver.class, HashicorpVaultClient.class})
@Extension(value = HashicorpVaultExtension.NAME)
public class HashicorpVaultExtension implements ServiceExtension {
    public static final String NAME = "Hashicorp Vault";

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    private Vault vault;
    private PrivateKeyResolver privateKeyResolver;
    private ScheduledExecutorService scheduledExecutorService;
    private HashicorpVaultClient hashicorpVaultClient;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault vault() {
        return vault;
    }

    @Provider
    public PrivateKeyResolver privateKeyResolver() {
        return privateKeyResolver;
    }

    @Provider
    public HashicorpVaultClient hashicorpVaultClient() {
        return hashicorpVaultClient;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), NAME);
        // TODO:
        var monitor = context.getMonitor();//.withPrefix(NAME);
        var config = HashicorpVaultConfig.create(context);
        initHashicorpVaultClient(config, monitor);
        vault = new HashicorpVault(hashicorpVaultClient, monitor);
        privateKeyResolver = new VaultPrivateKeyResolver(vault);
        context.registerService(CertificateResolver.class, new HashicorpCertificateResolver(vault, monitor));
    }

    @Override
    public void start() {
        if (hashicorpVaultClient.isTokenRenewable()) {
            var tokenRenewResult = hashicorpVaultClient.renewToken();

            if (tokenRenewResult.failed()) {
                throw new EdcException("[%s] Initial token renewal failed: %s".formatted(NAME, tokenRenewResult.getFailureDetail()));
            }

            hashicorpVaultClient.scheduleNextTokenRenewal();
        }
    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdownNow();
    }

    private void initHashicorpVaultClient(HashicorpVaultConfig config, Monitor monitor) {
        hashicorpVaultClient = new HashicorpVaultClient(config, httpClient, typeManager.getMapper(), scheduledExecutorService, monitor);

        var tokenLookUpResult = hashicorpVaultClient.lookUpToken();

        if (tokenLookUpResult.failed()) {
            throw new EdcException("[%s] Initial token look up failed: %s".formatted(NAME, tokenLookUpResult.getFailureDetail()));
        }

        monitor.info("Token is valid");
        hashicorpVaultClient.inspectToken();
    }
}
