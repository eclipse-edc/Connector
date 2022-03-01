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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import okhttp3.EventListener;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.BaseExtension;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@BaseExtension
@Provides({EventListener.class, ExecutorInstrumentation.class, MeterRegistry.class})
public class MicrometerExtension implements ServiceExtension {

    @EdcSetting
    public static final String ENABLE_METRICS = "edc.metrics.enabled";
    @EdcSetting
    public static final String ENABLE_SYSTEM_METRICS = "edc.metrics.system.enabled";
    @EdcSetting
    public static final String ENABLE_OKHTTP_METRICS = "edc.metrics.okhttp.enabled";
    @EdcSetting
    public static final String ENABLE_EXECUTOR_METRICS = "edc.metrics.executor.enabled";

    private static final String OKHTTP_REQUESTS_METRIC_NAME = "okhttp.requests";

    @Override
    public String name() {
        return "Micrometer Metrics";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        MeterRegistry registry = Metrics.globalRegistry;
        context.registerService(MeterRegistry.class, registry);

        var enableMetrics = context.getSetting(ENABLE_METRICS, true);
        var enableSystemMetrics = context.getSetting(ENABLE_SYSTEM_METRICS, true);
        var enableOkHttpMetrics = context.getSetting(ENABLE_OKHTTP_METRICS, true);
        var enableExecutorMetrics = context.getSetting(ENABLE_EXECUTOR_METRICS, true);

        if (!enableMetrics) {
            return; // metrics disabled
        }

        if (enableSystemMetrics) {
            enableSystemMetrics(registry);
        }

        if (enableOkHttpMetrics) {
            enableOkHttpMetrics(context, registry);
        }

        if (enableExecutorMetrics) {
            enableExecutorMetrics(context, registry);
        }
    }

    private void enableSystemMetrics(MeterRegistry registry) {
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
    }

    private void enableOkHttpMetrics(ServiceExtensionContext context, MeterRegistry registry) {
        var listener = OkHttpMetricsEventListener.builder(registry, OKHTTP_REQUESTS_METRIC_NAME).build();
        context.registerService(EventListener.class, listener);
    }

    private void enableExecutorMetrics(ServiceExtensionContext context, MeterRegistry registry) {
        context.registerService(ExecutorInstrumentation.class, new MicrometerExecutorInstrumentation(registry));
    }
}
