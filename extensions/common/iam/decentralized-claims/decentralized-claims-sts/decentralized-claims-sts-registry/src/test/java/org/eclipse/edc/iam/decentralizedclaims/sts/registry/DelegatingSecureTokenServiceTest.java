/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.sts.registry;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.sts.registry.DelegatingSecureTokenService.STS_TYPE_PROPERTY;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelegatingSecureTokenServiceTest {

    private static final String PARTICIPANT = "participant";
    private static final String DEFAULT_TYPE = "oauth";

    private final SecureTokenServiceRegistry registry = mock();
    private final ParticipantContextConfig participantContextConfig = mock();
    private final DelegatingSecureTokenService delegatingSts = new DelegatingSecureTokenService(registry, participantContextConfig, DEFAULT_TYPE);

    @Test
    void createToken_shouldResolveByConfiguredType_andDelegate() {
        var claims = Map.<String, Object>of("iss", "issuer");
        var delegate = mock(SecureTokenService.class);
        var token = TokenRepresentation.Builder.newInstance().token("token").build();
        when(participantContextConfig.getString(eq(PARTICIPANT), eq(STS_TYPE_PROPERTY), eq(DEFAULT_TYPE))).thenReturn("embedded");
        when(registry.resolve("embedded")).thenReturn(delegate);
        when(delegate.createToken(PARTICIPANT, claims, "scope")).thenReturn(Result.success(token));

        var result = delegatingSts.createToken(PARTICIPANT, claims, "scope");

        assertThat(result).isSucceeded().isSameAs(token);
        verify(delegate).createToken(PARTICIPANT, claims, "scope");
    }

    @Test
    void createToken_whenTypeNotSet_shouldFallBackToDefaultType() {
        var delegate = mock(SecureTokenService.class);
        when(participantContextConfig.getString(eq(PARTICIPANT), eq(STS_TYPE_PROPERTY), eq(DEFAULT_TYPE))).thenReturn(DEFAULT_TYPE);
        when(registry.resolve(DEFAULT_TYPE)).thenReturn(delegate);
        when(delegate.createToken(eq(PARTICIPANT), eq(Map.of()), eq(null))).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));

        var result = delegatingSts.createToken(PARTICIPANT, Map.of(), null);

        assertThat(result).isSucceeded();
        verify(registry).resolve(DEFAULT_TYPE);
    }

    @Test
    void createToken_whenTypeNotRegistered_shouldReturnFailure() {
        when(participantContextConfig.getString(eq(PARTICIPANT), eq(STS_TYPE_PROPERTY), eq(DEFAULT_TYPE))).thenReturn("unknown");
        when(registry.resolve("unknown")).thenReturn(null);

        var result = delegatingSts.createToken(PARTICIPANT, Map.of(), null);

        assertThat(result).isFailed().detail().contains("unknown");
    }

    @Test
    void createToken_whenDelegateFails_shouldPropagateFailure() {
        var delegate = mock(SecureTokenService.class);
        when(participantContextConfig.getString(eq(PARTICIPANT), eq(STS_TYPE_PROPERTY), eq(DEFAULT_TYPE))).thenReturn(DEFAULT_TYPE);
        when(registry.resolve(DEFAULT_TYPE)).thenReturn(delegate);
        when(delegate.createToken(eq(PARTICIPANT), eq(Map.of()), eq(null))).thenReturn(Result.failure("boom"));

        var result = delegatingSts.createToken(PARTICIPANT, Map.of(), null);

        assertThat(result).isFailed().detail().isEqualTo("boom");
        verify(delegate).createToken(PARTICIPANT, Map.of(), null);
    }

    @Test
    void createToken_whenTypeNotRegistered_shouldNotCallAnyDelegate() {
        var delegate = mock(SecureTokenService.class);
        when(participantContextConfig.getString(eq(PARTICIPANT), eq(STS_TYPE_PROPERTY), eq(DEFAULT_TYPE))).thenReturn("unknown");
        when(registry.resolve("unknown")).thenReturn(null);

        delegatingSts.createToken(PARTICIPANT, Map.of(), null);

        verify(delegate, never()).createToken(eq(PARTICIPANT), eq(Map.of()), eq(null));
    }
}
