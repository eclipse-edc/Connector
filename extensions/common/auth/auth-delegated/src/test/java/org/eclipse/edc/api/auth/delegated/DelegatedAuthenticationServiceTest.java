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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.api.auth.delegated.TestFunctions.createToken;
import static org.eclipse.edc.api.auth.delegated.TestFunctions.generateKey;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DelegatedAuthenticationServiceTest {

    private final TokenValidationRulesRegistry rulesRegistry = mock();
    private final PublicKeyResolver publicKeyResolver = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Monitor monitor = mock();
    private final JwkParser jwkParser = new JwkParser(mapper, monitor);
    private final DelegatedAuthenticationService service = new DelegatedAuthenticationService(publicKeyResolver, monitor, new TokenValidationServiceImpl(), rulesRegistry);

    @Test
    void isAuthenticated_valid() {
        var key = generateKey();
        var pk = jwkParser.parse(key.toPublicJWK().toJSONString());
        when(publicKeyResolver.resolveKey(anyString())).thenReturn(pk.map(k -> (PublicKey) k));

        var token = createToken(key);
        var headers = Map.of("Authorization", List.of("Bearer " + token));

        assertThat(service.isAuthenticated(headers)).isTrue();
        verify(publicKeyResolver).resolveKey(eq(key.getKeyID()));
        verify(rulesRegistry).getRules(eq(DelegatedAuthenticationService.MANAGEMENT_API_CONTEXT));
        verifyNoMoreInteractions(publicKeyResolver, rulesRegistry);
    }

    @Test
    void isAuthenticated_noHeaders() {
        assertThatThrownBy(() -> service.isAuthenticated(null))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(monitor).warning("Headers were null");
        verifyNoInteractions(rulesRegistry, publicKeyResolver);
    }

    @Test
    void isAuthenticated_emptyHeaders() {
        assertThatThrownBy(() -> service.isAuthenticated(Map.of()))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(monitor).warning("Header 'Authorization' not present");
        verifyNoInteractions(rulesRegistry, publicKeyResolver);
    }

    @Test
    void isAuthenticated_noAuthHeader() {
        assertThatThrownBy(() -> service.isAuthenticated(Map.of("foo", List.of("bar"))))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(monitor).warning("Header 'Authorization' not present");
        verifyNoInteractions(rulesRegistry, publicKeyResolver);
    }

    @Test
    void isAuthenticated_multipleAuthHeaders_shouldReject() {
        var key = generateKey();
        var token = createToken(key);

        var headers = Map.of("Authorization", List.of("Bearer " + token, "Bearer someOtherToken"));

        assertThat(service.isAuthenticated(headers)).isFalse();
        verify(monitor).warning(contains("Expected exactly 1 Authorization header, found 2"));
        verifyNoInteractions(rulesRegistry, publicKeyResolver);
    }

    @Test
    void isAuthenticated_notBearer() {
        var key = generateKey();
        var pk = jwkParser.parse(key.toPublicJWK().toJSONString());
        when(publicKeyResolver.resolveKey(anyString())).thenReturn(pk.map(k -> (PublicKey) k));

        var token = createToken(key);
        var headers = Map.of("Authorization", List.of(token));

        assertThat(service.isAuthenticated(headers)).isFalse();
        verify(monitor).warning("Authorization header must start with 'Bearer '");
        verifyNoInteractions(rulesRegistry, publicKeyResolver);
    }

}
