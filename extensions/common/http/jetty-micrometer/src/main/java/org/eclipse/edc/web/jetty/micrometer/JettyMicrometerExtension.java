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

package org.eclipse.edc.web.jetty.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.jetty.JettyService;

/**
 * An extension that registers Micrometer {@link JettyConnectionMetrics} into Jetty to
 * provide server metrics.
 */
@Extension(value = JettyMicrometerExtension.NAME)
public class JettyMicrometerExtension implements ServiceExtension {

    @Setting
    public static final String ENABLE_METRICS = "edc.metrics.enabled";
    @Setting
    public static final String ENABLE_JETTY_METRICS = "edc.metrics.jetty.enabled";
    public static final String NAME = "Jetty Micrometer Metrics";

    @Inject
    private JettyService jettyService;

    @Inject
    private MeterRegistry meterRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var enableMetrics = context.getSetting(ENABLE_METRICS, true);
        var enableJettyMetrics = context.getSetting(ENABLE_JETTY_METRICS, true);

        if (enableMetrics && enableJettyMetrics) {
            enableJettyConnectorMetrics();
        }
    }

    private void enableJettyConnectorMetrics() {
        jettyService.addConnectorConfigurationCallback(new JettyMicrometerConfiguration(meterRegistry));
    }
}
