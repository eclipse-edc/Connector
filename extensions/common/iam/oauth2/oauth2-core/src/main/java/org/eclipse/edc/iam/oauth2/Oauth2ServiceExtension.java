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

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.iam.oauth2.identity.IdentityProviderKeyResolver;
import org.eclipse.edc.iam.oauth2.identity.IdentityProviderKeyResolverConfiguration;
import org.eclipse.edc.iam.oauth2.identity.Oauth2ServiceImpl;
import org.eclipse.edc.iam.oauth2.jwt.Oauth2JwtDecoratorRegistryRegistryImpl;
import org.eclipse.edc.iam.oauth2.jwt.X509CertificateDecorator;
import org.eclipse.edc.iam.oauth2.rule.Oauth2ValidationRulesRegistryImpl;
import org.eclipse.edc.iam.oauth2.spi.CredentialsRequestAdditionalParametersProvider;
import org.eclipse.edc.iam.oauth2.spi.Oauth2AssertionDecorator;
import org.eclipse.edc.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.edc.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Provides OAuth2 client credentials flow support.
 */
@Provides({ IdentityService.class, Oauth2JwtDecoratorRegistry.class, Oauth2ValidationRulesRegistry.class })
@Extension(value = Oauth2ServiceExtension.NAME)
public class Oauth2ServiceExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Identity Service";
    private static final int DEFAULT_TOKEN_EXPIRATION = 5;
    @Setting
    private static final String PROVIDER_JWKS_URL = "edc.oauth.provider.jwks.url";
    @Setting(value = "outgoing tokens 'aud' claim value, by default it's the connector id")
    private static final String PROVIDER_AUDIENCE = "edc.oauth.provider.audience";
    @Setting(value = "incoming tokens 'aud' claim required value, by default it's the provider audience value")
    private static final String ENDPOINT_AUDIENCE = "edc.oauth.endpoint.audience";

    @Deprecated(since = "milestone8")
    @Setting
    private static final String DEPRECATED_PUBLIC_KEY_ALIAS = "edc.oauth.public.key.alias";
    @Setting
    private static final String PUBLIC_CERTIFICATE_ALIAS = "edc.oauth.certificate.alias";
    @Setting
    private static final String PRIVATE_KEY_ALIAS = "edc.oauth.private.key.alias";
    @Setting
    private static final String PROVIDER_JWKS_REFRESH = "edc.oauth.provider.jwks.refresh"; // in minutes
    @Setting
    private static final String TOKEN_URL = "edc.oauth.token.url";
    @Setting(value = "Token expiration in minutes. By default is 5 minutes")
    private static final String TOKEN_EXPIRATION = "edc.oauth.token.expiration"; // in minutes
    @Setting
    private static final String CLIENT_ID = "edc.oauth.client.id";
    @Setting
    private static final String NOT_BEFORE_LEEWAY = "edc.oauth.validation.nbf.leeway";
    private IdentityProviderKeyResolver providerKeyResolver;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private CertificateResolver certificateResolver;

    @Inject
    private Clock clock;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private CredentialsRequestAdditionalParametersProvider credentialsRequestAdditionalParametersProvider;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        var keyRefreshInterval = context.getSetting(PROVIDER_JWKS_REFRESH, 5);
        var identityProviderKeyResolverConfiguration = new IdentityProviderKeyResolverConfiguration(jwksUrl, keyRefreshInterval);
        providerKeyResolver = new IdentityProviderKeyResolver(context.getMonitor(), httpClient, typeManager, identityProviderKeyResolverConfiguration);

        var configuration = createConfig(context);

        var certificate = Optional.ofNullable(configuration.getCertificateResolver().resolveCertificate(configuration.getPublicCertificateAlias()))
                .orElseThrow(() -> new EdcException("Public certificate not found: " + configuration.getPublicCertificateAlias()));
        var jwtDecoratorRegistry = new Oauth2JwtDecoratorRegistryRegistryImpl();
        jwtDecoratorRegistry.register(new Oauth2AssertionDecorator(configuration.getProviderAudience(), configuration.getClientId(), clock, configuration.getTokenExpiration()));
        jwtDecoratorRegistry.register(new X509CertificateDecorator(certificate));
        context.registerService(Oauth2JwtDecoratorRegistry.class, jwtDecoratorRegistry);

        var validationRulesRegistry = new Oauth2ValidationRulesRegistryImpl(configuration, clock);
        context.registerService(Oauth2ValidationRulesRegistry.class, validationRulesRegistry);

        var privateKeyAlias = configuration.getPrivateKeyAlias();
        var privateKey = configuration.getPrivateKeyResolver().resolvePrivateKey(privateKeyAlias, PrivateKey.class);

        var oauth2Service = new Oauth2ServiceImpl(
                configuration,
                new TokenGenerationServiceImpl(privateKey),
                oauth2Client,
                jwtDecoratorRegistry,
                new TokenValidationServiceImpl(configuration.getIdentityProviderKeyResolver(), validationRulesRegistry),
                credentialsRequestAdditionalParametersProvider
        );

        context.registerService(IdentityService.class, oauth2Service);
    }

    @Override
    public void start() {
        providerKeyResolver.start();
    }

    @Override
    public void shutdown() {
        providerKeyResolver.stop();
    }

    private Oauth2ServiceConfiguration createConfig(ServiceExtensionContext context) {
        var providerAudience = context.getSetting(PROVIDER_AUDIENCE, context.getConnectorId());
        var endpointAudience = context.getSetting(ENDPOINT_AUDIENCE, providerAudience);
        var tokenUrl = context.getConfig().getString(TOKEN_URL);
        var publicCertificateAlias = getPublicCertificateAlias(context);
        var privateKeyAlias = context.getConfig().getString(PRIVATE_KEY_ALIAS);
        var clientId = context.getConfig().getString(CLIENT_ID);
        var tokenExpiration = context.getSetting(TOKEN_EXPIRATION, DEFAULT_TOKEN_EXPIRATION);
        return Oauth2ServiceConfiguration.Builder.newInstance()
                .identityProviderKeyResolver(providerKeyResolver)
                .tokenUrl(tokenUrl)
                .providerAudience(providerAudience)
                .endpointAudience(endpointAudience)
                .publicCertificateAlias(publicCertificateAlias)
                .privateKeyAlias(privateKeyAlias)
                .clientId(clientId)
                .privateKeyResolver(privateKeyResolver)
                .certificateResolver(certificateResolver)
                .notBeforeValidationLeeway(context.getSetting(NOT_BEFORE_LEEWAY, 10))
                .tokenExpiration(TimeUnit.MINUTES.toSeconds(tokenExpiration))
                .build();
    }

    private String getPublicCertificateAlias(ServiceExtensionContext context) {
        if (context.getConfig().hasPath(DEPRECATED_PUBLIC_KEY_ALIAS)) {
            context.getMonitor().warning(
                    format("Deprecated settings %s is being used for public certificate alias, please switch to the new settings %s",
                            DEPRECATED_PUBLIC_KEY_ALIAS, PUBLIC_CERTIFICATE_ALIAS));
            return context.getConfig().getString(DEPRECATED_PUBLIC_KEY_ALIAS);
        } else {
            return context.getConfig().getString(PUBLIC_CERTIFICATE_ALIAS);
        }
    }
}
