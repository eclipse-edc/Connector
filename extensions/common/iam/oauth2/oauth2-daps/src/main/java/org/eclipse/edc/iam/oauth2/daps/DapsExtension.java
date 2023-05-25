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

/**
 * Provides specialization of Oauth2 extension to interact with DAPS instance
 */
@Extension(value = DapsExtension.NAME)
public class DapsExtension implements ServiceExtension {

    public static final String NAME = "DAPS";
    public static final String DEFAULT_DAPS_TOKEN = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";
    @Setting(value = "The value of the scope claim that is passed to DAPS to obtain a DAT", defaultValue = DEFAULT_DAPS_TOKEN)
    public static final String DAPS_TOKEN_PROPERTY = "edc.iam.daps.token.name";

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
        var scope = context.getSetting(DAPS_TOKEN_PROPERTY, DEFAULT_DAPS_TOKEN);
        return new DapsTokenDecorator(scope);
    }
}
