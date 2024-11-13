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
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Configuration Extension for the STS OAuth2 client
 */
@Extension(StsRemoteClientConfigurationExtension.NAME)
public class StsRemoteClientConfigurationExtension implements ServiceExtension {

    protected static final String NAME = "Sts remote client configuration extension";
    @Inject
    private Vault vault;

    @Configuration
    private StsClientConfig clientConfig;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsRemoteClientConfiguration clientConfiguration(ServiceExtensionContext context) {
        return new StsRemoteClientConfiguration(clientConfig.tokenUrl(), clientConfig.clientId(), clientConfig.clientSecretAlias());
    }

    @Settings
    private record StsClientConfig(
            @Setting(key = "edc.iam.sts.oauth.token.url", description = "STS OAuth2 endpoint for requesting a token") String tokenUrl,
            @Setting(key = "edc.iam.sts.oauth.client.id", description = "STS OAuth2 client id") String clientId,
            @Setting(key = "edc.iam.sts.oauth.client.secret.alias", description = "Vault alias of STS OAuth2 client secret") String clientSecretAlias) {
    }
}

