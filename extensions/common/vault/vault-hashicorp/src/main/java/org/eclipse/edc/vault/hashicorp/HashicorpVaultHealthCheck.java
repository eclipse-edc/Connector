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

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;

import java.util.ArrayList;

/**
 * Implements the healthcheck of the Hashicorp Vault.
 * The healthcheck is a combination of:
 * <ol>
 *   <li>The actual Vault healthcheck which is performed by calling the healthcheck api of the Vault</li>
 *   <li>Token validation by calling the token lookup api of the Vault</li>
 * </ol>
 */
public class HashicorpVaultHealthCheck implements ReadinessProvider, LivenessProvider, StartupStatusProvider {

    private static final String DELIMITER = ", ";
    private final HashicorpVaultClient client;
    private final Monitor monitor;

    public HashicorpVaultHealthCheck(HashicorpVaultClient client, Monitor monitor) {
        this.client = client;
        this.monitor = monitor;
    }

    @Override
    public HealthCheckResult get() {

        var errors = new ArrayList<String>();

        var healthCheckResult = client.doHealthCheck();
        if (healthCheckResult.failed()) {
            errors.add(healthCheckResult.getFailureDetail());
        }

        // not retrying token lookup since the healthcheck is performed in short intervals (default 10s)
        var tokenLookUpResult = client.lookUpToken();
        if (tokenLookUpResult.failed()) {
            errors.add(tokenLookUpResult.getFailureDetail());
        }

        HealthCheckResult result;

        if (errors.isEmpty()) {
            result = HealthCheckResult.success();
        } else {
            monitor.warning("Healthcheck failed with reason(s): " + String.join(DELIMITER, errors));
            result = HealthCheckResult.failed(errors);
        }

        return result.forComponent(HashicorpVaultHealthExtension.NAME);
    }
}
