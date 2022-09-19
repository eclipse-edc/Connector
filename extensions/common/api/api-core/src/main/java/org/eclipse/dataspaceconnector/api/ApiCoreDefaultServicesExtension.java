/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api;

import org.eclipse.dataspaceconnector.api.auth.AllPassAuthenticationService;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides default service implementations for fallback
 */
public class ApiCoreDefaultServicesExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Api Core Default Services";
    }

    @Provider(isDefault = true)
    public AuthenticationService authenticationService(ServiceExtensionContext context) {
        context.getMonitor().warning("No AuthenticationService registered, an all-pass implementation will be used, not suitable for production environments");
        return new AllPassAuthenticationService();
    }

}
