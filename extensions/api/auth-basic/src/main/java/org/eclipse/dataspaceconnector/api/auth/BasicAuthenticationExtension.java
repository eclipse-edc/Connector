/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static java.lang.String.format;

/**
 * Extension that registers an AuthenticationService that uses API Keys
 */
@Provides(AuthenticationService.class)
public class BasicAuthenticationExtension implements ServiceExtension {

    @EdcSetting
    public static final String BASIC_AUTH = "edc.api.auth.basic";

    @Override
    public void initialize(ServiceExtensionContext context) {
        var basicAuthUsers = context.getConfig(BASIC_AUTH).getRelativeEntries();
        var monitor = context.getMonitor();

        // Register basic authentication filter
        if (!basicAuthUsers.isEmpty()) {
            var authService = new BasicAuthenticationService(basicAuthUsers, monitor);
            context.registerService(AuthenticationService.class, authService);
            monitor.info(format("API Authentication: basic auth configured with %s credential(s)", basicAuthUsers.size()));
        } else {
            monitor.warning("API Authentication: no basic auth credentials provided");
        }
    }
}
