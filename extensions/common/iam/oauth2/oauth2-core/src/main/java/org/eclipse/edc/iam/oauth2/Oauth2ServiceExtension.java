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
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
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
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * Provides OAuth2 client credentials flow support.
 *
 * @deprecated please switch to DCP.
 */
@Provides({ IdentityService.class })
@Extension(value = Oauth2ServiceExtension.NAME)
@Deprecated(since = "0.10.0")
public class Oauth2ServiceExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Identity Service";
    public static final String OAUTH2_TOKEN_CONTEXT = "oauth2";

    @Configuration
    private Oauth2ServiceConfiguration config;
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

        config = withDefaults(config, context);
        warnIfNoLeeway(config, context.getMonitor());

        var certificate = ofNullable(certificateResolver.resolveCertificate(config.getPublicCertificateAlias()))
                .orElseThrow(() -> new EdcException("Public certificate not found: " + config.getPublicCertificateAlias()));
        jwtDecoratorRegistry.register(OAUTH2_TOKEN_CONTEXT, Oauth2AssertionDecorator.Builder.newInstance()
                .audience(config.getProviderAudience())
                .clientId(config.getClientId())
                .clock(clock)
                .validity(config.getTokenExpiration())
                .build());
        jwtDecoratorRegistry.register(OAUTH2_TOKEN_CONTEXT, new X509CertificateDecorator(certificate));

        providerKeyResolver = identityProviderKeyResolver(context);

        var oauth2Service = createOauth2Service(config, jwtDecoratorRegistry, providerKeyResolver);
        context.registerService(IdentityService.class, oauth2Service);

        // add oauth2-specific validation rules
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new AudienceValidationRule(config.getEndpointAudience()));
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, config.getNotBeforeValidationLeeway(), false));
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, config.getIssuedAtLeeway(), false));
    }

    private Oauth2ServiceConfiguration withDefaults(Oauth2ServiceConfiguration config, ServiceExtensionContext context) {
        var providerAudience = ofNullable(config.getProviderAudience()).orElseGet(context::getComponentId);
        return config.toBuilder()
                .providerAudience(providerAudience)
                .endpointAudience(ofNullable(config.getEndpointAudience()).orElse(providerAudience))
                .build();
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
        return new IdentityProviderKeyResolver(context.getMonitor(), httpClient, typeManager, config.getJwksUrl(), config.getProviderJwksRefresh());
    }

    @NotNull
    private Oauth2ServiceImpl createOauth2Service(Oauth2ServiceConfiguration configuration,
                                                  TokenDecoratorRegistry jwtDecoratorRegistry,
                                                  IdentityProviderKeyResolver providerKeyResolver) {
        Supplier<String> privateKeySupplier = configuration::getPrivateKeyAlias;

        return new Oauth2ServiceImpl(
                configuration.getTokenUrl(),
                new JwtGenerationService(jwsSignerProvider),
                privateKeySupplier,
                oauth2Client,
                jwtDecoratorRegistry,
                tokenValidationRulesRegistry,
                tokenValidationService,
                providerKeyResolver,
                configuration.isTokenResourceEnabled()
        );
    }

    private void warnIfNoLeeway(Oauth2ServiceConfiguration configuration, Monitor monitor) {
        if (configuration.getIssuedAtLeeway() == 0) {
            var message = "No value was configured for '%s'. Consider setting a leeway of 2-5s in production to avoid problems with clock skew.".formatted(Oauth2ServiceConfiguration.ISSUED_AT_LEEWAY);
            monitor.info(message);
        }
    }

}
