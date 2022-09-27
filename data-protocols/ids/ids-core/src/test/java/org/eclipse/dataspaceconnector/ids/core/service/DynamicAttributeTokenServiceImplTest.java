/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.core.service;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicAttributeTokenServiceImplTest {
    
    private IdentityService identityService;
    private DynamicAttributeTokenServiceImpl tokenService;
    
    @BeforeEach
    void init() {
        identityService = mock(IdentityService.class);
        tokenService = new DynamicAttributeTokenServiceImpl(identityService);
    }
    
    @Test
    void obtainDynamicAttributeToken_tokenRequestSuccessful() {
        var recipient = "recipient";
        
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("token").build();
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.success(tokenRepresentation));
        
        var tokenResult = tokenService.obtainDynamicAttributeToken(recipient);
        
        assertThat(tokenResult.succeeded()).isTrue();
        assertThat(tokenResult.getContent().getTokenValue()).isEqualTo(tokenRepresentation.getToken());
        verify(identityService, times(1))
                .obtainClientCredentials(argThat(tokenParams -> recipient.equals(tokenParams.getAudience())));
    }
    
    @Test
    void obtainDynamicAttributeToken_tokenRequestFailed() {
        var recipient = "recipient";
    
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));
        
        var tokenResult = tokenService.obtainDynamicAttributeToken(recipient);
        
        assertThat(tokenResult.succeeded()).isFalse();
        verify(identityService, times(1))
                .obtainClientCredentials(argThat(tokenParams -> recipient.equals(tokenParams.getAudience())));
    }
    
    @Test
    void verifyDynamicAttributeToken_verificationSuccessful() {
        var issuer = URI.create("issuer");
        var audience = "audience";
        var token = getToken();
        
        var claimToken = ClaimToken.Builder.newInstance().build();
        when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
        
        var result = tokenService.verifyDynamicAttributeToken(token, issuer, audience);
    
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(claimToken);
        verify(identityService, times(1))
                .verifyJwtToken(argThat(getTokenRepresentationMatcher(token, issuer)), eq(audience));
    }
    
    @Test
    void verifyDynamicAttributeToken_verificationFailed() {
        var issuer = URI.create("issuer");
        var audience = "audience";
        var token = getToken();
        
        when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.failure("error"));
        
        var result = tokenService.verifyDynamicAttributeToken(token, issuer, audience);
        
        assertThat(result.succeeded()).isFalse();
        verify(identityService, times(1))
                .verifyJwtToken(argThat(getTokenRepresentationMatcher(token, issuer)), eq(audience));
    }
    
    private DynamicAttributeToken getToken() {
        return new DynamicAttributeTokenBuilder()
                ._tokenFormat_(TokenFormat.JWT)
                ._tokenValue_("token")
                .build();
    }
    
    private ArgumentMatcher<TokenRepresentation> getTokenRepresentationMatcher(DynamicAttributeToken token, URI issuer) {
        return tokenRepresentation -> tokenRepresentation.getToken().equals(token.getTokenValue()) &&
                tokenRepresentation.getAdditional().get("issuerConnector").equals(issuer);
    }
    
}
