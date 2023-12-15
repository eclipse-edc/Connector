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
import org.eclipse.edc.runtime.metamodel.annotation.Requires;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_ENABLED;
import static org.eclipse.edc.vault.hashicorp.model.Constants.VAULT_HEALTH_CHECK_ENABLED_DEFAULT;

@Requires(HealthCheckService.class)
@Extension(value = HashicorpVaultHealthExtension.NAME)
public class HashicorpVaultHealthExtension implements ServiceExtension {

    public static final String NAME = "Hashicorp Vault Health";

    @Inject
    private HealthCheckService healthCheckService;

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private HashicorpVaultClient client;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix(NAME);
        var healthCheckEnabled = context.getSetting(VAULT_HEALTH_CHECK_ENABLED, VAULT_HEALTH_CHECK_ENABLED_DEFAULT);
        if (healthCheckEnabled) {
            var healthCheck = new HashicorpVaultHealthCheck(client, context.getMonitor());
            healthCheckService.addLivenessProvider(healthCheck);
            healthCheckService.addReadinessProvider(healthCheck);
            healthCheckService.addStartupStatusProvider(healthCheck);
            monitor.info("Healthcheck initialization complete");
        } else {
            monitor.info("Healthcheck disabled");
        }
    }
}