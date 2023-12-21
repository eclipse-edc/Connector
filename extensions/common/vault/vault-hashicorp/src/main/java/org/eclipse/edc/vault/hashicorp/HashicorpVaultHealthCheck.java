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

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;

import java.util.ArrayList;

public class HashicorpVaultHealthCheck implements ReadinessProvider, LivenessProvider, StartupStatusProvider {

    private static final String HEALTH_CHECK_ERROR = "Failed to perform Healthcheck";
    private static final String HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE = "Healthcheck unsuccessful: %s %s";
    private static final String DELIMITER = ", ";
    private final HashicorpVaultClient client;
    private final Monitor monitor;

    public HashicorpVaultHealthCheck(HashicorpVaultClient client, Monitor monitor) {
        this.client = client;
        this.monitor = monitor;
    }

    @Override
    public HealthCheckResult get() {

        var healthCheckResponseResult = client.doHealthCheck();
        if (healthCheckResponseResult.failed()) {
            monitor.severe(HEALTH_CHECK_ERROR);
            return HealthCheckResult
                    .failed(healthCheckResponseResult.getFailureMessages())
                    .forComponent(HashicorpVaultHealthExtension.NAME);
        }

        var errors = new ArrayList<String>(0);
        var response = healthCheckResponseResult.getContent();

        switch (response.getCodeAsEnum()) {
            case INITIALIZED_UNSEALED_AND_ACTIVE -> {
                // do nothing
            }
            case UNSEALED_AND_STANDBY -> {
                var standbyMsg = HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE.formatted("Vault is in standby", response.getPayload());
                errors.add(standbyMsg);
            }
            case DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE -> {
                var recoveryModeMsg = HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE.formatted("Vault is in recovery mode", response.getPayload());
                errors.add(recoveryModeMsg);
            }
            case PERFORMANCE_STANDBY -> {
                var performanceStandbyMsg = HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE.formatted("Vault is in performance standby", response.getPayload());
                errors.add(performanceStandbyMsg);
            }
            case NOT_INITIALIZED -> {
                var notInitializedMsg = HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE.formatted("Vault is not initialized", response.getPayload());
                errors.add(notInitializedMsg);
            }
            case SEALED -> {
                var sealedMsg = HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE.formatted("Vault is sealed", response.getPayload());
                errors.add(sealedMsg);
            }
            default -> {
                var unspecifiedMsg = HEALTH_CHECK_UNSUCCESSFUL_TEMPLATE.formatted("Unspecified response from vault. Code: " + response.getCode(), response.getPayload());
                errors.add(unspecifiedMsg);
            }
        }

        var tokenLookUpResult = client.lookUpToken();

        if (tokenLookUpResult.failed()) {
            var tokenNotValidMsg = "Token look up failed: %s".formatted(tokenLookUpResult.getFailureDetail());
            errors.add(tokenNotValidMsg);
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
