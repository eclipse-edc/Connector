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

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

public class DelegatedAuthenticationService implements AuthenticationService {

    public static final String MANAGEMENT_API_CONTEXT = "management-api";
    private final PublicKeyResolver publicKeyResolver;
    private final Monitor monitor;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry rulesRegistry;

    public DelegatedAuthenticationService(PublicKeyResolver publicKeyResolver,
                                          long cacheValidityMs,
                                          Monitor monitor,
                                          TokenValidationService tokenValidationService,
                                          TokenValidationRulesRegistry rulesRegistry) {
        this.publicKeyResolver = publicKeyResolver;
        this.monitor = monitor;
        this.tokenValidationService = tokenValidationService;
        this.rulesRegistry = rulesRegistry;
    }

    @Override
    public boolean isAuthenticated(Map<String, List<String>> headers) {

        if (headers == null || headers.isEmpty()) {
            var msg = "Headers were null or empty";
            monitor.warning(msg);
            throw new AuthenticationFailedException(msg);
        }

        var authHeaders = headers.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(AUTHORIZATION))
                .map(headers::get)
                .findFirst();

        return authHeaders.map(this::performTokenValidation).orElseThrow(() -> {
            var msg = "Header '%s' not present";
            monitor.warning(msg);
            return new AuthenticationFailedException(msg.formatted(AUTHORIZATION));
        });

    }

    public void updateCache() {

    }

    private boolean performTokenValidation(List<String> authHeaders) {
        if (authHeaders.size() != 1) {
            monitor.warning("Expected exactly 1 Authorization header, found %d".formatted(authHeaders.size()));
            return false;
        }
        var token = authHeaders.get(0);
        if (!token.toLowerCase().startsWith("bearer ")) {
            monitor.warning("Authorization header must start with 'bearer '");
            return false;
        }
        token = token.substring(6).trim(); // "bearer" has 7 characters, it could be upper case, lower case or capitalized

        var rules = rulesRegistry.getRules(MANAGEMENT_API_CONTEXT);
        return tokenValidationService.validate(token, publicKeyResolver, rules).succeeded();
    }

}
