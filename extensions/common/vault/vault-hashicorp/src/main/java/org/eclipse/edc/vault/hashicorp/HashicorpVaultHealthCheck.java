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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.eclipse.edc.vault.hashicorp.model.HealthCheckResponse;

import java.util.ArrayList;

import static java.lang.String.format;

public class HashicorpVaultHealthCheck implements ReadinessProvider, LivenessProvider, StartupStatusProvider {

    private static final String HEALTH_CHECK_ERROR_TEMPLATE = "Healthcheck unsuccessful: %s %s";
    private static final String COMPONENT_NAME = "Hashicorp Vault";
    private final HashicorpVaultClient client;
    private final Monitor monitor;

    public HashicorpVaultHealthCheck(HashicorpVaultClient client, Monitor monitor) {
        this.client = client;
        this.monitor = monitor;
    }

    @Override
    public HealthCheckResult get() {

        HealthCheckResponse response;
        var errors = new ArrayList<String>(0);

        try {
            response = client.doHealthCheck();
        } catch (EdcException e) {  // can be thrown by the client, e.g. on JSON parsing error, etc.
            var exceptionMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "EdcException: " + e.getMessage(), "");
            monitor.severe(exceptionMsg, e);
            return HealthCheckResult.failed(exceptionMsg).forComponent(COMPONENT_NAME);
        }

        switch (response.getCodeAsEnum()) {
            case INITIALIZED_UNSEALED_AND_ACTIVE -> {
                // do nothing
            }
            case UNSEALED_AND_STANDBY -> {
                var standbyMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "Vault is in standby", response.getPayload());
                monitor.warning(standbyMsg);
                errors.add(standbyMsg);
            }
            case DISASTER_RECOVERY_MODE_REPLICATION_SECONDARY_AND_ACTIVE -> {
                var recoveryModeMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "Vault is in recovery mode", response.getPayload());
                monitor.warning(recoveryModeMsg);
                errors.add(recoveryModeMsg);
            }
            case PERFORMANCE_STANDBY -> {
                var performanceStandbyMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "Vault is in performance standby", response.getPayload());
                monitor.warning(performanceStandbyMsg);
                errors.add(performanceStandbyMsg);
            }
            case NOT_INITIALIZED -> {
                var notInitializedMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "Vault is not initialized", response.getPayload());
                monitor.warning(notInitializedMsg);
                errors.add(notInitializedMsg);
            }
            case SEALED -> {
                var sealedMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "Vault is sealed", response.getPayload());
                monitor.warning(sealedMsg);
                errors.add(sealedMsg);
            }
            default -> {
                var unspecifiedMsg = format(HEALTH_CHECK_ERROR_TEMPLATE, "Unspecified response from vault. Code: " + response.getCode(), response.getPayload());
                monitor.warning(unspecifiedMsg);
                errors.add(unspecifiedMsg);
            }
        }

        var tokenLookUpResult = client.lookUpToken();

        if (tokenLookUpResult.failed()) {
            var tokenNotValidMsg = "Token look up failed: %s".formatted(tokenLookUpResult.getFailureDetail());
            monitor.warning(tokenNotValidMsg);
            errors.add(tokenNotValidMsg);
        }

        var result = errors.isEmpty() ? HealthCheckResult.success() : HealthCheckResult.failed(errors);
        return result.forComponent(COMPONENT_NAME);
    }
}
