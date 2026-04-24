/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.api.management.authn;

import org.eclipse.edc.api.authentication.filter.JwtValidatorFilter;
import org.eclipse.edc.api.authentication.filter.ServicePrincipalAuthenticationFilter;
import org.eclipse.edc.keys.resolver.JwksPublicKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.IssuerEqualsValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.time.Clock;
import java.util.List;

import static org.eclipse.edc.connector.api.management.authn.ManagementApiOauth2AuthenticationExtension.NAME;

@Extension(NAME)
public class ManagementApiOauth2AuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Management API OAuth2 Authentication Extension";
    private static final long FIVE_MINUTES = 1000 * 60 * 5;
    @Inject
    private WebService webService;
    @Inject
    private Clock clock;
    @Inject
    private KeyParserRegistry keyParserRegistry;
    @Inject
    private ParticipantContextService participantContextService;
    @Configuration
    private OauthConfiguration oauthConfiguration;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var alias = ApiContext.MANAGEMENT;
        webService.registerResource(alias, new ServicePrincipalAuthenticationFilter(participantContextService));
        webService.registerResource(alias, new JwtValidatorFilter(tokenValidationService, JwksPublicKeyResolver.create(keyParserRegistry, oauthConfiguration.jwksUrl(), monitor, oauthConfiguration.cacheValidityInMillis), getRules()));
    }

    private List<TokenValidationRule> getRules() {
        return List.of(
                new IssuerEqualsValidationRule(oauthConfiguration.expectedIssuer),
                new NotBeforeValidationRule(clock, 0, true),
                new ExpirationIssuedAtValidationRule(clock, 0, false)
        );
    }

    @Settings
    record OauthConfiguration(
            @Setting(key = "edc.iam.oauth2.issuer", description = "Issuer of the OAuth2 server", required = false)
            String expectedIssuer,
            @Setting(key = "edc.iam.oauth2.jwks.url", description = "Absolute URL where the JWKS of the OAuth2 server is hosted")
            String jwksUrl,
            @Setting(key = "edc.iam.oauth2.jwks.cache.validity", description = "Time (in ms) that cached JWKS are cached", defaultValue = "" + FIVE_MINUTES)
            long cacheValidityInMillis
    ) {

    }
}
