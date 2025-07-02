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
 *       Dawex Systems - Add audience validation
 *
 */

package org.eclipse.edc.api.auth.delegated;

import org.eclipse.edc.api.auth.spi.ApiAuthenticationProvider;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.time.Clock;

import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_CACHE_TIME_TO_LIVE;
import static org.eclipse.edc.api.auth.delegated.DelegatedAuthenticationService.MANAGEMENT_API_CONTEXT;

/**
 * Extension that registers an AuthenticationService that delegates authentication and authorization to a third-party IdP
 * and register an {@link ApiAuthenticationProvider} under the type called delegated
 */
@Extension(value = DelegatedAuthenticationExtension.NAME)
public class DelegatedAuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Delegating Authentication Service Extension";
    private static final int DEFAULT_VALIDATION_TOLERANCE = 5_000;
    private static final String AUTH_KEY = "auth";
    private static final String CONFIG_ALIAS = "web.http.<context>." + AUTH_KEY + ".";
    private static final String DELEGATED_TYPE = "delegated";
    @Deprecated(since = "0.12.0", forRemoval = true)
    private static final String KEY_URL_PROPERTY = "edc.api.auth.dac.key.url";
    @Deprecated(since = "0.12.0", forRemoval = true)
    private static final String DEPRECATED_AUTH_CACHE_VALIDITY = "edc.api.auth.dac.cache.validity";

    @Setting(context = CONFIG_ALIAS, description = "URL where the third-party IdP's public key(s) can be resolved for the configured <context>")
    public static final String AUTH_KEY_URL = "dac.key.url";
    @Setting(context = CONFIG_ALIAS, description = "Duration (in ms) that the internal key cache is valid for the configured <context>", type = "Long", defaultValue = "" + DEFAULT_CACHE_TIME_TO_LIVE)
    public static final String AUTH_CACHE_VALIDITY_MS = "dac.cache.validity";
    @Setting(description = "Default token validation time tolerance (in ms), e.g. for nbf or exp claims", defaultValue = "" + DEFAULT_VALIDATION_TOLERANCE, key = "edc.api.auth.dac.validation.tolerance")
    private int validationTolerance;
    @Deprecated(since = "0.12.0", forRemoval = true)
    @Setting(description = "Duration (in ms) that the internal key cache is valid", defaultValue = "" + DEFAULT_CACHE_TIME_TO_LIVE, key = DEPRECATED_AUTH_CACHE_VALIDITY, required = false)
    private long cacheValidityMs;
    @Deprecated(since = "0.12.0", forRemoval = true)
    @Setting(description = "URL where the third-party IdP's public key(s) can be resolved", key = KEY_URL_PROPERTY, required = false)
    private String keyUrl;
    public static final String AUDIENCE_KEY = "dac.audience";
    @Setting(context = CONFIG_ALIAS, description = "Expected audience in the token received by the api management", key = "web.http.management." + AUTH_KEY + "." + AUDIENCE_KEY, required = false)
    private String audience;

    @Inject
    private ApiAuthenticationProviderRegistry providerRegistry;
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

        if (keyUrl != null) {
            var message = "Settings %s and %s have been removed".formatted(KEY_URL_PROPERTY, DEPRECATED_AUTH_CACHE_VALIDITY) +
                    ", to configure delegated authentication for management api please configure it properly through the " +
                    "`web.http.management.auth.%s` and `web.http.management.auth.%s` settings".formatted(AUTH_KEY_URL, AUTH_CACHE_VALIDITY_MS);
            monitor.severe(message);
            throw new EdcException(message);
        }

        if (audience == null) {
            monitor.warning("No audience configured for delegated authentication, defaulting to the participantId");
            audience = context.getParticipantId();
        }

        tokenValidationRulesRegistry.addRule(MANAGEMENT_API_CONTEXT, new AudienceValidationRule(audience));
        tokenValidationRulesRegistry.addRule(MANAGEMENT_API_CONTEXT, new NotBeforeValidationRule(clock, validationTolerance, true));
        tokenValidationRulesRegistry.addRule(MANAGEMENT_API_CONTEXT, new ExpirationIssuedAtValidationRule(clock, validationTolerance, true));

        providerRegistry.register(DELEGATED_TYPE, (cfg) -> delegatedProvider(monitor, cfg));
    }

    public Result<AuthenticationService> delegatedProvider(Monitor monitor, Config config) {
        var keyUrl = config.getString(AUTH_KEY_URL);
        var cacheValidityMs = config.getLong(AUTH_CACHE_VALIDITY_MS, DEFAULT_CACHE_TIME_TO_LIVE);
        var resolver = JwksPublicKeyResolver.create(keyParserRegistry, keyUrl, monitor, cacheValidityMs);

        return Result.success(new DelegatedAuthenticationService(resolver, monitor, tokenValidationService, tokenValidationRulesRegistry));
    }
}
