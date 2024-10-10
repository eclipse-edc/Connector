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

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenDecoratorRegistry;

/**
 * Provides specialization of Oauth2 extension to interact with DAPS instance
 *
 * @deprecated will be removed in the next versions.
 */
@Extension(value = DapsExtension.NAME)
@Deprecated(since = "0.10.0")
public class DapsExtension implements ServiceExtension {

    public static final String NAME = "DAPS";
    public static final String DEFAULT_TOKEN_SCOPE = "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL";
    @Setting(value = "The value of the scope claim that is passed to DAPS to obtain a DAT", defaultValue = DEFAULT_TOKEN_SCOPE)
    public static final String DAPS_TOKEN_SCOPE = "edc.iam.token.scope";
    public static final String OAUTH_2_DAPS_TOKEN_CONTEXT = "oauth2-daps";

    @Inject
    private TokenDecoratorRegistry jwtDecoratorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.getMonitor().warning("The extension %s has been deprecated, please switch to a decentralized implementation".formatted(NAME));
        jwtDecoratorRegistry.register(OAUTH_2_DAPS_TOKEN_CONTEXT, new DapsJwtDecorator());
    }

    @Provider
    public TokenDecorator createDapsTokenDecorator(ServiceExtensionContext context) {
        var scope = context.getConfig().getString(DAPS_TOKEN_SCOPE);

        return new DapsTokenDecorator(scope);
    }
}
