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

package org.eclipse.edc.api;

import org.eclipse.edc.api.auth.spi.AllPassAuthenticationService;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link ApiCoreExtension}
 */
@Provides(AuthenticationService.class)
public class ApiCoreDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Api Core Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public AuthenticationService authenticationService(ServiceExtensionContext context) {
        context.getMonitor().warning("No AuthenticationService registered, an all-pass implementation will be used, not suitable for production environments");
        return new AllPassAuthenticationService();
    }

}
