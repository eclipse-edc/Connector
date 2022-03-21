/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.daps;

import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides specialization of Oauth2 extension to interact with DAPS instance
 */
public class DapsExtension implements ServiceExtension {

    @Inject
    private Oauth2JwtDecoratorRegistry jwtDecoratorRegistry;

    @Override
    public String name() {
        return "DAPS";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        jwtDecoratorRegistry.register(new DapsJwtDecorator());
    }
}
