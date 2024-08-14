/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.remote.client;

import org.eclipse.edc.iam.identitytrust.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Configuration Extension for the STS OAuth2 client
 */
@Extension(StsRemoteClientConfigurationExtension.NAME)
public class StsRemoteClientConfigurationExtension implements ServiceExtension {

    @Setting(value = "STS OAuth2 endpoint for requesting a token")
    public static final String TOKEN_URL = "edc.iam.sts.oauth.token.url";

    @Setting(value = "STS OAuth2 client id")
    public static final String CLIENT_ID = "edc.iam.sts.oauth.client.id";

    @Setting(value = "Vault alias of STS OAuth2 client secret")
    public static final String CLIENT_SECRET_ALIAS = "edc.iam.sts.oauth.client.secret.alias";

    protected static final String NAME = "Sts remote client configuration extension";

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsRemoteClientConfiguration clientConfiguration(ServiceExtensionContext context) {

        var tokenUrl = context.getConfig().getString(TOKEN_URL);
        var clientId = context.getConfig().getString(CLIENT_ID);
        var clientSecretAlias = context.getConfig().getString(CLIENT_SECRET_ALIAS);

        return new StsRemoteClientConfiguration(tokenUrl, clientId, clientSecretAlias);
    }

}
