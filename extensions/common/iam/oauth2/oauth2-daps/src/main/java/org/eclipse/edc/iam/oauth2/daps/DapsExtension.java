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

package org.eclipse.edc.iam.oauth2.daps;

import org.eclipse.edc.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static java.lang.String.format;

/**
 * Provides specialization of Oauth2 extension to interact with DAPS instance
 */
@Extension(value = DapsExtension.NAME)
@Deprecated(since = "0.1.0")
public class DapsExtension implements ServiceExtension {

    public static final String NAME = "DAPS";
    public static final String DEFAULT_TOKEN_SCOPE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";
    @Setting(value = "The value of the scope claim that is passed to DAPS to obtain a DAT", defaultValue = DEFAULT_TOKEN_SCOPE)
    public static final String DAPS_TOKEN_SCOPE = "edc.iam.token.scope";

    @Inject
    private Oauth2JwtDecoratorRegistry jwtDecoratorRegistry;


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        jwtDecoratorRegistry.register(new DapsJwtDecorator());
    }

    @Provider
    public TokenDecorator createDapsTokenDecorator(ServiceExtensionContext context) {
        var scope = context.getSetting(DAPS_TOKEN_SCOPE, null);
        if (scope == null) {
            context.getMonitor().warning(() -> format("The config value '%s' was not supplied, falling back to the default '%s'. " +
                    "Please be aware that this default will be removed in future releases", DAPS_TOKEN_SCOPE, DEFAULT_TOKEN_SCOPE));
            scope = DEFAULT_TOKEN_SCOPE;
        }

        return new DapsTokenDecorator(scope);
    }
}
