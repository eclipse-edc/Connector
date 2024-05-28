/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.boot.health;

import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.HealthStatus;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.eclipse.edc.spi.system.health.ReadinessProvider;
import org.eclipse.edc.spi.system.health.StartupStatusProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public class HealthCheckServiceImpl implements HealthCheckService {
    private final List<LivenessProvider> livenessProviders;
    private final List<ReadinessProvider> readinessProviders;
    private final List<StartupStatusProvider> startupStatusProviders;


    public HealthCheckServiceImpl() {
        readinessProviders = new CopyOnWriteArrayList<>();
        livenessProviders = new CopyOnWriteArrayList<>();
        startupStatusProviders = new CopyOnWriteArrayList<>();
    }

    private static @NotNull Function<Supplier<HealthCheckResult>, HealthCheckResult> getSilent() {
        return supplier -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                return HealthCheckResult.Builder.newInstance().component(supplier.getClass().getName()).failure(e.getMessage()).build();
            }
        };
    }

    @Override
    public void addLivenessProvider(LivenessProvider provider) {
        livenessProviders.add(provider);
    }

    @Override
    public void addReadinessProvider(ReadinessProvider provider) {
        readinessProviders.add(provider);
    }

    @Override
    public void addStartupStatusProvider(StartupStatusProvider provider) {
        startupStatusProviders.add(provider);
    }

    @Override
    public HealthStatus isLive() {
        return new HealthStatus(livenessProviders.stream().map(getSilent()).toList());
    }

    @Override
    public HealthStatus isReady() {
        return new HealthStatus(readinessProviders.stream().map(getSilent()).toList());
    }

    @Override
    public HealthStatus getStartupStatus() {
        return new HealthStatus(startupStatusProviders.stream().map(getSilent()).toList());
    }
}
