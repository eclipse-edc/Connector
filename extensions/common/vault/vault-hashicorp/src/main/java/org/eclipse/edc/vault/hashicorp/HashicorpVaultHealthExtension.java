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
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

@Requires(HealthCheckService.class)
public class HashicorpVaultHealthExtension implements ServiceExtension {
    public static final boolean VAULT_HEALTH_CHECK_DEFAULT = true;

    @Setting(value = "Whether or not the vault health check is enabled", defaultValue = "true", type = "boolean")
    public static final String VAULT_HEALTH_CHECK = "edc.vault.hashicorp.health.check.enabled";

    @Inject
    private HealthCheckService healthCheckService;

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Override
    public String name() {
        return "Hashicorp Vault Health Check";
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = HashicorpVaultClientConfig.create(context);
        var client = new HashicorpVaultClient(config, httpClient, typeManager.getMapper());
        configureHealthCheck(client, context);

        context.getMonitor().info("HashicorpVaultExtension: health check initialization complete.");
    }

    private void configureHealthCheck(HashicorpVaultClient client, ServiceExtensionContext context) {
        var healthCheckEnabled =
                context.getSetting(VAULT_HEALTH_CHECK, VAULT_HEALTH_CHECK_DEFAULT);
        if (!healthCheckEnabled) return;

        var healthCheck =
                new HashicorpVaultHealthCheck(client, context.getMonitor());

        healthCheckService.addLivenessProvider(healthCheck);
        healthCheckService.addReadinessProvider(healthCheck);
        healthCheckService.addStartupStatusProvider(healthCheck);
    }
}
