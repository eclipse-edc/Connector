/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.iam.oauth2;

import com.microsoft.dagx.iam.oauth2.impl.IdentityProviderKeyResolver;
import com.microsoft.dagx.iam.oauth2.impl.OAuth2Configuration;
import com.microsoft.dagx.iam.oauth2.impl.OAuth2ServiceImpl;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import okhttp3.OkHttpClient;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides OAuth2 client credentials flow support.
 */
public class OAuth2Extension implements ServiceExtension {

    @DagxSetting
    private static final String PROVIDER_JWKS_URL = "dagx.oauth.provider.jwks.url";

    @DagxSetting
    private static final String PROVIDER_AUDIENCE = "dagx.oauth.provider.audience";

    @DagxSetting
    private static final String PUBLIC_KEY_ALIAS = "dagx.oauth.public.key.alias";

    @DagxSetting
    private static final String PRIVATE_KEY_ALIAS = "dagx.oauth.private.key.alias";

    @DagxSetting
    private static final String PROVIDER_JWKS_REFRESH = "dagx.oauth.provider.jwks.refresh"; // in minutes

    @DagxSetting
    private static final String TOKEN_URL = "dagx.oauth.token.url";

    @DagxSetting
    private static final String CLIENT_ID = "dagx.oauth.client.id";

    private IdentityProviderKeyResolver providerKeyResolver;

    private long keyRefreshInterval;

    private ScheduledExecutorService executorService;

    @Override
    public Set<String> provides() {
        return Set.of("iam", "oauth2");
    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:http-client");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var httpClient = context.getService(OkHttpClient.class);

        // setup the provider key resolver, which will be scheduled for refresh at runtime start
        String jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        providerKeyResolver = new IdentityProviderKeyResolver(jwksUrl, context.getMonitor(), httpClient);
        keyRefreshInterval = Integer.parseInt(context.getSetting(PROVIDER_JWKS_REFRESH, "5"));

        // setup the OAuth2 service
        OAuth2Configuration.Builder configBuilder = OAuth2Configuration.Builder.newInstance();

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

        OAuth2Configuration configuration = configBuilder.build();

        IdentityService oAuth2Service = new OAuth2ServiceImpl(configuration);

        context.registerService(IdentityService.class, oAuth2Service);

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
