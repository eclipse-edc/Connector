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

package org.eclipse.edc.iam.decentralizedclaims.sts.remote.client;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Configuration Extension for the STS OAuth2 client
 */
@Extension(StsRemoteClientExtension.NAME)
public class StsRemoteClientExtension implements ServiceExtension {

    protected static final String NAME = "Sts remote client extension";

    @Setting(description = "STS OAuth2 endpoint for requesting a token")
    private static final String TOKEN_URL = "edc.iam.sts.oauth.token.url";
    @Setting(description = "STS OAuth2 client id")
    private static final String CLIENT_ID = "edc.iam.sts.oauth.client.id";
    @Setting(description = "Vault alias of STS OAuth2 client secret")
    private static final String CLIENT_SECRET_ALIAS = "edc.iam.sts.oauth.client.secret.alias";
    
    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private ParticipantContextConfig participantContextConfig;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SecureTokenService secureTokenService() {
        return new RemoteSecureTokenService(oauth2Client, this::clientConfiguration, vault);
    }

    private StsRemoteClientConfiguration clientConfiguration(String participantContextId) {
        return new StsRemoteClientConfiguration(
                participantContextConfig.getString(participantContextId, TOKEN_URL),
                participantContextConfig.getString(participantContextId, CLIENT_ID),
                participantContextConfig.getString(participantContextId, CLIENT_SECRET_ALIAS));
    }
}
