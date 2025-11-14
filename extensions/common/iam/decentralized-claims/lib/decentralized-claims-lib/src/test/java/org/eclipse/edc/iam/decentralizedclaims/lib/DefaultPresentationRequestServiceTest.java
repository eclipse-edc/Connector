/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.lib;

import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createPresentationContainer;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultPresentationRequestServiceTest {

    private String participantContextId = "pcId";
    private String did = "did:web:one";
    private String counterPartyDid = "did:web:two";
    private String counterPartyToken = "siToken";
    private List<String> scopes = List.of("scope1", "scope2");

    private SecureTokenService secureTokenService = mock();
    private CredentialServiceUrlResolver credentialServiceUrlResolver = mock();
    private CredentialServiceClient credentialServiceClient = mock();

    private DefaultPresentationRequestService service = new DefaultPresentationRequestService(secureTokenService, credentialServiceUrlResolver, credentialServiceClient);

    @Test
    void requestPresentation_shouldReturnPresentation() {
        var token = "token";
        var tr = TokenRepresentation.Builder.newInstance().token(token).build();
        var url = "http://url";
        var presentation = createPresentationContainer();

        when(secureTokenService.createToken(any(), any(), any())).thenReturn(Result.success(tr));
        when(credentialServiceUrlResolver.resolve(any())).thenReturn(Result.success(url));
        when(credentialServiceClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(Result.success(List.of(presentation)));

        var result = service.requestPresentation(participantContextId, did, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(presentation);

        verify(secureTokenService, times(1)).createToken(eq(participantContextId),
                argThat(claims -> claims.get(AUDIENCE).equals(counterPartyDid) && claims.get(ISSUER).equals(did) && claims.get(SUBJECT).equals(did)),
                isNull());
        verify(credentialServiceUrlResolver, times(1)).resolve(counterPartyDid);
        verify(credentialServiceClient, times(1)).requestPresentation(url, token, scopes);
    }

    @Test
    void requestPresentation_creatingSiTokenFails_shouldReturnFailure() {
        when(secureTokenService.createToken(any(), any(), any())).thenReturn(Result.failure("error"));

        var result = service.requestPresentation(participantContextId, did, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isFailed();

        verifyNoInteractions(credentialServiceUrlResolver);
        verifyNoInteractions(credentialServiceClient);
    }

    @Test
    void requestPresentation_urlNotResolvable_shouldReturnFailure() {
        var token = "token";
        var tr = TokenRepresentation.Builder.newInstance().token(token).build();

        when(secureTokenService.createToken(any(), any(), any())).thenReturn(Result.success(tr));
        when(credentialServiceUrlResolver.resolve(any())).thenReturn(Result.failure("error"));

        var result = service.requestPresentation(participantContextId, did, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isFailed();

        verifyNoInteractions(credentialServiceClient);
    }

    @Test
    void requestPresentation_presentationRequestFails_shouldReturnFailure() {
        var token = "token";
        var tr = TokenRepresentation.Builder.newInstance().token(token).build();
        var url = "http://url";

        when(secureTokenService.createToken(any(), any(), any())).thenReturn(Result.success(tr));
        when(credentialServiceUrlResolver.resolve(any())).thenReturn(Result.success(url));
        when(credentialServiceClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(Result.failure("error"));

        var result = service.requestPresentation(participantContextId, did, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isFailed();
    }
}
