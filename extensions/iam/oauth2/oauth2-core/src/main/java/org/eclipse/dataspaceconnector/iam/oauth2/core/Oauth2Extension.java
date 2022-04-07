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
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationServiceImpl;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.identity.IdentityProviderKeyResolver;
import org.eclipse.dataspaceconnector.iam.oauth2.core.identity.Oauth2ServiceImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.DefaultJwtDecorator;
import org.eclipse.dataspaceconnector.iam.oauth2.core.jwt.Oauth2JwtDecoratorRegistryRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.Oauth2ValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides OAuth2 client credentials flow support.
 */
@Provides({IdentityService.class, Oauth2JwtDecoratorRegistry.class, Oauth2ValidationRulesRegistry.class})
public class Oauth2Extension implements ServiceExtension {

    private static final long TOKEN_EXPIRATION = TimeUnit.MINUTES.toSeconds(5);

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

    @EdcSetting
    private static final String NOT_BEFORE_LEEWAY = "edc.oauth.validation.nbf.leeway";

    private IdentityProviderKeyResolver providerKeyResolver;

    private long keyRefreshInterval;

    private ScheduledExecutorService executorService;

    @Inject
    private OkHttpClient okHttpClient;

    @Override
    public String name() {
        return "OAuth2";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        providerKeyResolver = new IdentityProviderKeyResolver(jwksUrl, context.getMonitor(), okHttpClient, context.getTypeManager());
        keyRefreshInterval = context.getSetting(PROVIDER_JWKS_REFRESH, 5);

        var configuration = createConfig(context);

        var defaultDecorator = new DefaultJwtDecorator(configuration.getProviderAudience(), configuration.getClientId(), getEncodedClientCertificate(configuration), TOKEN_EXPIRATION);
        var jwtDecoratorRegistry = new Oauth2JwtDecoratorRegistryRegistryImpl();
        jwtDecoratorRegistry.register(defaultDecorator);
        context.registerService(Oauth2JwtDecoratorRegistry.class, jwtDecoratorRegistry);

        var validationRulesRegistry = new Oauth2ValidationRulesRegistryImpl(configuration);
        context.registerService(Oauth2ValidationRulesRegistry.class, validationRulesRegistry);

        var tokenValidationService = new TokenValidationServiceImpl(configuration.getIdentityProviderKeyResolver(), validationRulesRegistry);

        var privateKeyAlias = configuration.getPrivateKeyAlias();
        var privateKey = configuration.getPrivateKeyResolver().resolvePrivateKey(privateKeyAlias, PrivateKey.class);
        var tokenGenerationService = new TokenGenerationServiceImpl(privateKey);

        var oauth2Service = new Oauth2ServiceImpl(configuration, tokenGenerationService, okHttpClient, jwtDecoratorRegistry, context.getTypeManager(), tokenValidationService);

        context.registerService(IdentityService.class, oauth2Service);
    }

    @Override
    public void start() {
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

    private static byte[] getEncodedClientCertificate(Oauth2Configuration configuration) {
        var certificate = configuration.getCertificateResolver().resolveCertificate(configuration.getPublicCertificateAlias());
        if (certificate == null) {
            throw new EdcException("Public certificate not found: " + configuration.getPublicCertificateAlias());
        }

        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new EdcException("Failed to encode certificate: " + e);
        }
    }

    private Oauth2Configuration createConfig(ServiceExtensionContext context) {
        var providerAudience = context.getSetting(PROVIDER_AUDIENCE, context.getConnectorId());
        var tokenUrl = context.getConfig().getString(TOKEN_URL);
        var publicKeyAlias = context.getConfig().getString(PUBLIC_KEY_ALIAS);
        var privateKeyAlias = context.getConfig().getString(PRIVATE_KEY_ALIAS);
        var clientId = context.getConfig().getString(CLIENT_ID);
        var privateKeyResolver = context.getService(PrivateKeyResolver.class);
        var certificateResolver = context.getService(CertificateResolver.class);
        return Oauth2Configuration.Builder.newInstance()
                .identityProviderKeyResolver(providerKeyResolver)
                .tokenUrl(tokenUrl)
                .providerAudience(providerAudience)
                .publicCertificateAlias(publicKeyAlias)
                .privateKeyAlias(privateKeyAlias)
                .clientId(clientId)
                .privateKeyResolver(privateKeyResolver)
                .certificateResolver(certificateResolver)
                .notBeforeValidationLeeway(context.getSetting(NOT_BEFORE_LEEWAY, 10))
                .build();
    }
}
