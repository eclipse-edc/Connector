/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.authz;

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.authorization.filter.RoleBasedAccessFeature;
import org.eclipse.edc.api.authorization.filter.ScopeBasedAccessFeature;
import org.eclipse.edc.api.authorization.service.AuthorizationServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.connector.api.management.authz.ManagementApiAuthorizationExtension.NAME;

@Extension(NAME)
public class ManagementApiAuthorizationExtension implements ServiceExtension {

    public static final String NAME = "Management API Authorization Extension";
    @Inject
    private WebService webService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var alias = ApiContext.MANAGEMENT;
        webService.registerResource(alias, new RoleBasedAccessFeature());
        webService.registerResource(alias, new ScopeBasedAccessFeature());
    }

    @Provider
    public AuthorizationService authorizationService() {
        return new AuthorizationServiceImpl();
    }


}
