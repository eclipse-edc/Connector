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

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultClient;

/**
 * Implements the healthcheck of the Hashicorp Vault.
 * The healthcheck is a combination of:
 * <ol>
 *   <li>The actual Vault healthcheck which is performed by calling the healthcheck api of the Vault</li>
 *   <li>Token validation by calling the token lookup api of the Vault</li>
 * </ol>
 */
public class HashicorpVaultHealthCheck implements ReadinessProvider, LivenessProvider, StartupStatusProvider {
    private final HashicorpVaultClient client;
    private final Monitor monitor;

    public HashicorpVaultHealthCheck(HashicorpVaultClient client, Monitor monitor) {
        this.client = client;
        this.monitor = monitor;
    }

    @Override
    public HealthCheckResult get() {
        return client
                .doHealthCheck()
                .merge(client.isTokenRenewable())
                .flatMap(result -> {
                    if (result.succeeded()) {
                        return HealthCheckResult.success();
                    } else {
                        monitor.debug("Vault health check failed with reason(s): " + result.getFailureDetail());
                        return HealthCheckResult.failed(result.getFailureMessages());
                    }
                }).forComponent(HashicorpVaultHealthExtension.NAME);
    }
}
