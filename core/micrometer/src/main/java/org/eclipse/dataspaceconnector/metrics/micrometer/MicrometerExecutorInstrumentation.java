/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * {@link ExecutorInstrumentation} that decorates executors using wrappers
 * provided by Micrometer {@link ExecutorServiceMetrics} to report metrics such as thread pool
 * size and execution timings.
 */
public class MicrometerExecutorInstrumentation implements ExecutorInstrumentation {
    private final MeterRegistry registry;

    public MicrometerExecutorInstrumentation(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ScheduledExecutorService instrument(ScheduledExecutorService target, String name) {
        return ExecutorServiceMetrics.monitor(registry, target, name);
    }

    @Override
    public ExecutorService instrument(ExecutorService target, String name) {
        return ExecutorServiceMetrics.monitor(registry, target, name);
    }
}
