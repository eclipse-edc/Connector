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
 *       Metaform Systems, Inc. - configurable API contexts
 *
 */

package org.eclipse.edc.web.jersey.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * An extension that register a Micrometer {@link MetricsApplicationEventListener} into Jersey to
 * provide metrics and request timings, for the API contexts configured in {@link #JERSEY_METRICS_CONTEXTS}.
 */
public class JerseyMicrometerExtension implements ServiceExtension {

    static final String ALL_CONTEXTS = "*";
    private static final String DEFAULT_CONTEXT = "default";

    @Setting
    public static final String ENABLE_METRICS = "edc.metrics.enabled";
    @Setting
    public static final String ENABLE_JERSEY_METRICS = "edc.metrics.jersey.enabled";
    @Setting(description = "Comma-separated aliases of the API contexts whose requests are measured, e.g. 'default,management,protocol', or '*' for all of them",
            defaultValue = DEFAULT_CONTEXT)
    public static final String JERSEY_METRICS_CONTEXTS = "edc.metrics.jersey.contexts";

    @Inject
    private WebService webService;

    @Inject
    private MeterRegistry meterRegistry;

    @Inject
    private PortMappingRegistry portMappingRegistry;

    private Monitor monitor;
    private boolean enabled;
    private Set<String> contexts;

    @Override
    public String name() {
        return "Jersey Micrometer Metrics";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        enabled = context.getSetting(ENABLE_METRICS, true) && context.getSetting(ENABLE_JERSEY_METRICS, true);
        contexts = Arrays.stream(context.getSetting(JERSEY_METRICS_CONTEXTS, DEFAULT_CONTEXT).split(","))
                .map(String::trim)
                .filter(alias -> !alias.isEmpty())
                .collect(toSet());
    }

    /**
     * The listeners are registered once every extension is initialized, so that the port mappings of all the API
     * contexts are known, and before the web service starts.
     */
    @Override
    public void prepare() {
        if (enabled) {
            aliasesToMeasure().forEach(alias -> webService.registerResource(alias, new MetricsApplicationEventListener(
                    meterRegistry,
                    new DefaultJerseyTagsProvider(),
                    "jersey",
                    true)));
        }
    }

    private List<String> aliasesToMeasure() {
        var known = portMappingRegistry.getAll().stream().map(PortMapping::name).collect(toSet());
        if (contexts.contains(ALL_CONTEXTS)) {
            return known.stream().sorted().toList();
        }
        // a resource registered for an alias without port mapping would make the web server fail at startup
        contexts.stream().filter(alias -> !known.contains(alias))
                .forEach(alias -> monitor.warning("No API context '%s' is configured: its requests are not measured (%s)".formatted(alias, JERSEY_METRICS_CONTEXTS)));
        return contexts.stream().filter(known::contains).sorted().toList();
    }
}
