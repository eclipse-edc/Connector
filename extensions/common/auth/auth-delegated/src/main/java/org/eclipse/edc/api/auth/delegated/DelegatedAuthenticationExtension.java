/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.auth.delegated;

import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.time.Clock;

import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_CACHE_TIME_TO_LIVE;
import static org.eclipse.edc.api.auth.delegated.DelegatedAuthenticationService.MANAGEMENT_API_CONTEXT;

/**
 * Extension that registers an AuthenticationService that delegates authentication and authorization to a third-party IdP
 */
@Extension(value = DelegatedAuthenticationExtension.NAME)
public class DelegatedAuthenticationExtension implements ServiceExtension {

    public static final int DEFAULT_VALIDATION_TOLERANCE = 5_000;
    public static final String NAME = "Delegating Authentication Service Extension";
    @Setting(value = "Duration (in ms) that the internal key cache is valid", type = "Long", defaultValue = "" + DEFAULT_CACHE_TIME_TO_LIVE)
    public static final String AUTH_SETTING_CACHE_VALIDITY_MS = "edc.api.auth.dac.cache.validity";
    @Setting(value = "URL where the third-party IdP's public key(s) can be resolved")
    public static final String AUTH_SETTING_KEY_URL = "edc.api.auth.dac.key.url";
    @Setting(value = "Default token validation time tolerance (in ms), e.g. for nbf or exp claims", defaultValue = "" + DEFAULT_VALIDATION_TOLERANCE)
    private static final String AUTH_SETTING_VALIDATION_TOLERANCE_MS = "edc.api.auth.dac.validation.tolerance";
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;
    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;
    @Inject
    private KeyParserRegistry keyParserRegistry;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("Delegated API Authentication");

        var keyUrl = context.getConfig().getString(AUTH_SETTING_KEY_URL, null);
        if (keyUrl == null) {
            monitor.warning("The '%s' setting was not provided, so the DelegatedAuthenticationService will NOT be registered. In this case, the TokenBasedAuthenticationService usually acts as fallback.".formatted(AUTH_SETTING_KEY_URL));
            return;
        }
        var cacheValidityMs = context.getConfig().getLong(AUTH_SETTING_CACHE_VALIDITY_MS, DEFAULT_CACHE_TIME_TO_LIVE);
        var tolerance = context.getConfig().getInteger(AUTH_SETTING_VALIDATION_TOLERANCE_MS, DEFAULT_VALIDATION_TOLERANCE);

        //todo: currently, only JWKS urls are supported
        var resolver = JwksPublicKeyResolver.create(keyParserRegistry, keyUrl, monitor, cacheValidityMs);

        tokenValidationRulesRegistry.addRule(MANAGEMENT_API_CONTEXT, new NotBeforeValidationRule(clock, tolerance, true));
        tokenValidationRulesRegistry.addRule(MANAGEMENT_API_CONTEXT, new ExpirationIssuedAtValidationRule(clock, tolerance, true));

        // always register - this would potentially overwrite other services
        authenticationRegistry.register("management-api", new DelegatedAuthenticationService(resolver, monitor, tokenValidationService, tokenValidationRulesRegistry));
    }
}
