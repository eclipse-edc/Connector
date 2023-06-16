/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.api.observability;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = ObservabilityApiExtension.NAME)
public class ObservabilityApiExtension implements ServiceExtension {

    public static final String NAME = "Observability API";
    private final HealthCheckResult result = HealthCheckResult.Builder.newInstance().component(NAME).build();

    @Inject
    private WebService webService;

    @Inject
    private HealthCheckService healthCheckService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(new ObservabilityApiController(healthCheckService));

        healthCheckService.addReadinessProvider(() -> result);
        healthCheckService.addLivenessProvider(() -> result);
    }

}
