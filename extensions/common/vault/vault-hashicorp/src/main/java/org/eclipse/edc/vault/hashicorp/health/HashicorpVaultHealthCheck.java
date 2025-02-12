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
 *       Cofinity-X - implement extensible authentication
 *
 */

package org.eclipse.edc.vault.hashicorp.health;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultHealthService;

/**
 * Implements the healthcheck of the Hashicorp Vault. The health check is performed by calling
 * the healthcheck API of the vault.
 */
public class HashicorpVaultHealthCheck implements ReadinessProvider, LivenessProvider, StartupStatusProvider {
    private final HashicorpVaultHealthService healthService;
    private final Monitor monitor;

    public HashicorpVaultHealthCheck(HashicorpVaultHealthService healthService, Monitor monitor) {
        this.healthService = healthService;
        this.monitor = monitor;
    }

    @Override
    public HealthCheckResult get() {
        return healthService
                .doHealthCheck()
                .flatMap(result -> {
                    var statusBuilder = HealthCheckResult.Builder.newInstance().component("HashicorpVault");
                    if (result.succeeded()) {
                        return statusBuilder.success().build();
                    } else {
                        monitor.debug("Vault health check failed with reason(s): " + result.getFailureDetail());
                        return statusBuilder.failure(result.getFailureMessages()).build();
                    }
                }).forComponent(HashicorpVaultHealthExtension.NAME);
    }
}
