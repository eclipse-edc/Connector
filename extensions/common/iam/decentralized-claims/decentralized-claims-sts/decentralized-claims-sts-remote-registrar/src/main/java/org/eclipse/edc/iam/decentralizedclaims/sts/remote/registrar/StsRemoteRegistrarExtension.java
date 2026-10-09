/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.sts.remote.registrar;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigEntry;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigView;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantContextConfigValidatorRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry.STS_TYPE_CONFIG_KEY;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.httpUrl;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.notBlank;

/**
 * Registers the remote (OAuth2) {@link RemoteSecureTokenService} into the {@link SecureTokenServiceRegistry} bound to
 * the {@code "oauth"} type, so it can be selected dynamically per participant context.
 */
@Extension(StsRemoteRegistrarExtension.NAME)
public class StsRemoteRegistrarExtension implements ServiceExtension {

    public static final String NAME = "Sts remote registrar extension";
    public static final String OAUTH_STS_TYPE = "oauth";

    @Setting(description = "STS OAuth2 endpoint for requesting a token")
    private static final String TOKEN_URL = "edc.iam.sts.oauth.token.url";
    @Setting(description = "STS OAuth2 client id")
    private static final String CLIENT_ID = "edc.iam.sts.oauth.client.id";
    @Setting(description = "Vault alias of STS OAuth2 client secret")
    private static final String CLIENT_SECRET_ALIAS = "edc.iam.sts.oauth.client.secret.alias";

    @Inject
    private SecureTokenServiceRegistry secureTokenServiceRegistry;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private ParticipantContextConfig participantContextConfig;

    @Inject
    private Vault vault;

    @Inject
    private ParticipantContextConfigValidatorRegistry configValidatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        secureTokenServiceRegistry.register(OAUTH_STS_TYPE, new RemoteSecureTokenService(oauth2Client, this::clientConfiguration, vault));

        configValidatorRegistry.register(ParticipantConfigEntry.Builder.newInstance(TOKEN_URL)
                .description("STS OAuth2 endpoint for requesting a token").validator(httpUrl()).requiredWhen(this::isOauthSts).build());
        configValidatorRegistry.register(ParticipantConfigEntry.Builder.newInstance(CLIENT_ID)
                .description("STS OAuth2 client id").validator(notBlank()).requiredWhen(this::isOauthSts).build());
        configValidatorRegistry.register(ParticipantConfigEntry.Builder.newInstance(CLIENT_SECRET_ALIAS)
                .description("Vault alias of STS OAuth2 client secret").validator(notBlank()).requiredWhen(this::isOauthSts).build());
    }

    private boolean isOauthSts(ParticipantConfigView config) {
        return OAUTH_STS_TYPE.equals(config.getEntry(STS_TYPE_CONFIG_KEY).orElseGet(secureTokenServiceRegistry::defaultType));
    }

    private StsRemoteClientConfiguration clientConfiguration(String participantContextId) {
        return new StsRemoteClientConfiguration(
                participantContextConfig.getString(participantContextId, TOKEN_URL),
                participantContextConfig.getString(participantContextId, CLIENT_ID),
                participantContextConfig.getString(participantContextId, CLIENT_SECRET_ALIAS));
    }
}
