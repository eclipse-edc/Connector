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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.iam.oauth2.impl.IdentityProviderKeyResolver;
import org.eclipse.dataspaceconnector.iam.oauth2.impl.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.impl.Oauth2ServiceImpl;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides OAuth2 client credentials flow support.
 */
public class Oauth2Extension implements ServiceExtension {

    @EdcSetting
    private static final String PROVIDER_JWKS_URL = "edc.oauth.provider.jwks.url";

    @EdcSetting
    private static final String PROVIDER_AUDIENCE = "edc.oauth.provider.audience";

    @EdcSetting
    private static final String PUBLIC_KEY_ALIAS = "edc.oauth.public.key.alias";

    @EdcSetting
    private static final String PRIVATE_KEY_ALIAS = "edc.oauth.private.key.alias";

    @EdcSetting
    private static final String PROVIDER_JWKS_REFRESH = "edc.oauth.provider.jwks.refresh"; // in minutes

    @EdcSetting
    private static final String TOKEN_URL = "edc.oauth.token.url";

    @EdcSetting
    private static final String CLIENT_ID = "edc.oauth.client.id";

    private IdentityProviderKeyResolver providerKeyResolver;

    private long keyRefreshInterval;

    private ScheduledExecutorService executorService;

    @Override
    public Set<String> provides() {
        return Set.of(IdentityService.FEATURE, "oauth2");
    }

    @Override
    public Set<String> requires() {
        return Set.of("dataspaceconnector:http-client");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var httpClient = context.getService(OkHttpClient.class);

        // setup the provider key resolver, which will be scheduled for refresh at runtime start
        String jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        providerKeyResolver = new IdentityProviderKeyResolver(jwksUrl, context.getMonitor(), httpClient);
        keyRefreshInterval = Integer.parseInt(context.getSetting(PROVIDER_JWKS_REFRESH, "5"));

        // setup the OAuth2 service
        Oauth2Configuration.Builder configBuilder = Oauth2Configuration.Builder.newInstance();

        configBuilder.identityProviderKeyResolver(providerKeyResolver);

        String tokenUrl = context.getSetting(TOKEN_URL, "tokenUrl");
        configBuilder.tokenUrl(tokenUrl);

        String providerAudience = context.getSetting(PROVIDER_AUDIENCE, "providerAudience");
        configBuilder.providerAudience(providerAudience);

        String publicKeyAlias = context.getSetting(PUBLIC_KEY_ALIAS, "publicKeyAlias");
        configBuilder.publicCertificateAlias(publicKeyAlias);

        String privateKeyAlias = context.getSetting(PRIVATE_KEY_ALIAS, "privateKeyAlias");
        configBuilder.privateKeyAlias(privateKeyAlias);

        String clientId = context.getSetting(CLIENT_ID, "clientId");
        configBuilder.clientId(clientId);

        PrivateKeyResolver privateKeyResolver = context.getService(PrivateKeyResolver.class);
        configBuilder.privateKeyResolver(privateKeyResolver);

        CertificateResolver certificateResolver = context.getService(CertificateResolver.class);
        configBuilder.certificateResolver(certificateResolver);

        configBuilder.objectMapper(context.getTypeManager().getMapper());

        Oauth2Configuration configuration = configBuilder.build();

        IdentityService oauth2Service = new Oauth2ServiceImpl(configuration);

        context.registerService(IdentityService.class, oauth2Service);

        context.getMonitor().info("Initialized OAuth2 extension");
    }

    @Override
    public void start() {
        // refresh the provider keys at start, then schedule a refresh on a periodic basis according to the configured interval
        providerKeyResolver.refreshKeys();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> providerKeyResolver.refreshKeys(), keyRefreshInterval, keyRefreshInterval, TimeUnit.MINUTES);
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
