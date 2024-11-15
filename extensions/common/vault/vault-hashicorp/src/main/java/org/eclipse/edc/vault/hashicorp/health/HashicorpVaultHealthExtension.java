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

package org.eclipse.edc.vault.hashicorp.health;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultClient;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultSettings;


@Requires(HealthCheckService.class)
@Extension(value = HashicorpVaultHealthExtension.NAME)
public class HashicorpVaultHealthExtension implements ServiceExtension {

    public static final String NAME = "Hashicorp Vault Health";

    @Inject
    private HealthCheckService healthCheckService;

    @Inject
    private HashicorpVaultClient client;

    @Configuration
    private HashicorpVaultSettings settings;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix(NAME);
        if (settings.healthCheckEnabled()) {
            var healthCheck = new HashicorpVaultHealthCheck(client, monitor);
            healthCheckService.addLivenessProvider(healthCheck);
            healthCheckService.addReadinessProvider(healthCheck);
            healthCheckService.addStartupStatusProvider(healthCheck);
            monitor.debug("Vault health check initialization complete");
        } else {
            monitor.debug("Vault health check disabled");
        }
    }
}
