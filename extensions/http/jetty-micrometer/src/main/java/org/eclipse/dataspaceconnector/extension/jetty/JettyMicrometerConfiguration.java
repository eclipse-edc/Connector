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

package org.eclipse.dataspaceconnector.extension.jetty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics;
import org.eclipse.jetty.server.ServerConnector;

import java.util.function.Consumer;

/**
 * Configuration callback to instrument Jetty server metrics with Micrometer.
 */
public class JettyMicrometerConfiguration implements Consumer<ServerConnector> {
    private final MeterRegistry registry;

    public JettyMicrometerConfiguration(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void accept(ServerConnector connector) {
        connector.addBean(new JettyConnectionMetrics(registry, connector, Tags.empty()));
    }
}
