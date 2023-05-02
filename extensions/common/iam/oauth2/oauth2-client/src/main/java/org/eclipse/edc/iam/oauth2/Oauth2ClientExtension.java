/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.iam.oauth2.client.Oauth2ClientImpl;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = Oauth2ClientExtension.NAME)
@Provides(Oauth2Client.class)
public class Oauth2ClientExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Client";

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Oauth2Client oauth2Client(ServiceExtensionContext context) {
        return new Oauth2ClientImpl(httpClient, typeManager);
    }
}
