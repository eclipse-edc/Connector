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
 *       sovity GmbH - added issuedAt leeway
 *
 */

package org.eclipse.edc.iam.oauth2;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.identity.IdentityProviderKeyResolver;
import org.eclipse.edc.iam.oauth2.identity.Oauth2ServiceImpl;
import org.eclipse.edc.iam.oauth2.jwt.X509CertificateDecorator;
import org.eclipse.edc.iam.oauth2.spi.Oauth2AssertionDecorator;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.keys.spi.CertificateResolver;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenDecoratorRegistry;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Provides OAuth2 client credentials flow support.
 */
@Provides({ IdentityService.class })
@Extension(value = Oauth2ServiceExtension.NAME)
public class Oauth2ServiceExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Identity Service";
    public static final String OAUTH2_TOKEN_CONTEXT = "oauth2";
    private static final int DEFAULT_TOKEN_EXPIRATION = 5;
    @Setting
    private static final String PROVIDER_JWKS_URL = "edc.oauth.provider.jwks.url";
    @Setting(value = "outgoing tokens 'aud' claim value, by default it's the connector id")
    private static final String PROVIDER_AUDIENCE = "edc.oauth.provider.audience";
    @Setting(value = "incoming tokens 'aud' claim required value, by default it's the provider audience value")
    private static final String ENDPOINT_AUDIENCE = "edc.oauth.endpoint.audience";

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
    @Setting(value = "Leeway in seconds for validating the not before (nbf) claim in the token.", defaultValue = "10", type = "int")
    private static final String NOT_BEFORE_LEEWAY = "edc.oauth.validation.nbf.leeway";
    @Setting(value = "Leeway in seconds for validating the issuedAt claim in the token. By default it is 0 seconds.", defaultValue = "0", type = "int")
    private static final String ISSUED_AT_LEEWAY = "edc.oauth.validation.issued.at.leeway";
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
    private TypeManager typeManager;

    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;

    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private TokenDecoratorRegistry jwtDecoratorRegistry;
    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var configuration = createConfig(context);

        var certificate = Optional.ofNullable(certificateResolver.resolveCertificate(configuration.getPublicCertificateAlias()))
                .orElseThrow(() -> new EdcException("Public certificate not found: " + configuration.getPublicCertificateAlias()));
        jwtDecoratorRegistry.register(OAUTH2_TOKEN_CONTEXT, new Oauth2AssertionDecorator(configuration.getProviderAudience(), configuration.getClientId(), clock, configuration.getTokenExpiration()));
        jwtDecoratorRegistry.register(OAUTH2_TOKEN_CONTEXT, new X509CertificateDecorator(certificate));

        providerKeyResolver = identityProviderKeyResolver(context);

        var oauth2Service = createOauth2Service(configuration, jwtDecoratorRegistry, providerKeyResolver);
        context.registerService(IdentityService.class, oauth2Service);

        // add oauth2-specific validation rules
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new AudienceValidationRule(configuration.getEndpointAudience()));
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, configuration.getNotBeforeValidationLeeway()));
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, configuration.getIssuedAtLeeway()));
    }

    @Override
    public void start() {
        providerKeyResolver.start();
    }

    @Override
    public void shutdown() {
        providerKeyResolver.stop();
    }

    private IdentityProviderKeyResolver identityProviderKeyResolver(ServiceExtensionContext context) {
        var jwksUrl = context.getSetting(PROVIDER_JWKS_URL, "http://localhost/empty_jwks_url");
        var keyRefreshInterval = context.getSetting(PROVIDER_JWKS_REFRESH, 5);
        return new IdentityProviderKeyResolver(context.getMonitor(), httpClient, typeManager, jwksUrl, keyRefreshInterval);
    }

    @NotNull
    private Oauth2ServiceImpl createOauth2Service(Oauth2ServiceConfiguration configuration,
                                                  TokenDecoratorRegistry jwtDecoratorRegistry,
                                                  IdentityProviderKeyResolver providerKeyResolver) {
        Supplier<String> privateKeySupplier = configuration::getPrivateKeyAlias;

        return new Oauth2ServiceImpl(
                configuration,
                new JwtGenerationService(jwsSignerProvider),
                privateKeySupplier,
                oauth2Client,
                jwtDecoratorRegistry,
                tokenValidationRulesRegistry,
                tokenValidationService,
                providerKeyResolver
        );
    }

    private Oauth2ServiceConfiguration createConfig(ServiceExtensionContext context) {
        var providerAudience = context.getSetting(PROVIDER_AUDIENCE, context.getComponentId());
        var endpointAudience = context.getSetting(ENDPOINT_AUDIENCE, providerAudience);
        var tokenUrl = context.getConfig().getString(TOKEN_URL);
        var publicCertificateAlias = context.getConfig().getString(PUBLIC_CERTIFICATE_ALIAS);
        var privateKeyAlias = context.getConfig().getString(PRIVATE_KEY_ALIAS);
        var clientId = context.getConfig().getString(CLIENT_ID);
        var tokenExpiration = context.getSetting(TOKEN_EXPIRATION, DEFAULT_TOKEN_EXPIRATION);
        return Oauth2ServiceConfiguration.Builder.newInstance()
                .tokenUrl(tokenUrl)
                .providerAudience(providerAudience)
                .endpointAudience(endpointAudience)
                .publicCertificateAlias(publicCertificateAlias)
                .privateKeyAlias(privateKeyAlias)
                .clientId(clientId)
                .notBeforeValidationLeeway(context.getSetting(NOT_BEFORE_LEEWAY, 10))
                .issuedAtLeeway(getIssuedAtLeeway(context))
                .tokenExpiration(TimeUnit.MINUTES.toSeconds(tokenExpiration))
                .build();
    }

    private int getIssuedAtLeeway(ServiceExtensionContext context) {
        if (!context.getConfig().hasKey(ISSUED_AT_LEEWAY)) {
            var message = format(
                    "No value was configured for '%s'. Consider setting a leeway of 2-5s in production to avoid problems with clock skew.",
                    ISSUED_AT_LEEWAY
            );
            context.getMonitor().info(message);
        }

        return context.getSetting(ISSUED_AT_LEEWAY, 0);
    }

}
