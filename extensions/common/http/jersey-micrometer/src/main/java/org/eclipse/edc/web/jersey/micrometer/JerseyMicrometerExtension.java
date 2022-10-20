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

package org.eclipse.edc.web.jersey.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

/**
 * An extension that register a Micrometer {@link MetricsApplicationEventListener} into Jersey to
 * provide metrics and request timings.
 */
public class JerseyMicrometerExtension implements ServiceExtension {

    @Setting
    public static final String ENABLE_METRICS = "edc.metrics.enabled";
    @Setting
    public static final String ENABLE_JERSEY_METRICS = "edc.metrics.jersey.enabled";

    @Inject
    private WebService webService;

    @Inject
    private MeterRegistry meterRegistry;

    @Override
    public String name() {
        return "Jersey Micrometer Metrics";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var enableMetrics = context.getSetting(ENABLE_METRICS, true);
        var enableJerseyMetrics = context.getSetting(ENABLE_JERSEY_METRICS, true);

        if (enableMetrics && enableJerseyMetrics) {
            enableJerseyControllerMetrics();
        }
    }

    private void enableJerseyControllerMetrics() {
        webService.registerResource(new MetricsApplicationEventListener(
                meterRegistry,
                new DefaultJerseyTagsProvider(),
                "jersey",
                true));
    }
}
