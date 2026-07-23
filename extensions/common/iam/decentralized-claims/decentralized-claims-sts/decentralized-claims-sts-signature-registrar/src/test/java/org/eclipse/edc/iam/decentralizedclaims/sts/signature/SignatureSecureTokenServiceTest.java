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

package org.eclipse.edc.iam.decentralizedclaims.sts.signature;

import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.iam.decentralizedclaims.sts.signature.SignatureSecureTokenService.SIGNATURE_KEY_NAME;
import static org.eclipse.edc.iam.decentralizedclaims.sts.signature.SignatureSecureTokenService.SIGNATURE_KID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignatureSecureTokenServiceTest {

    private static final String PARTICIPANT = "participant-1";
    private static final String KEY_NAME = "signing-key";
    private static final String KID = "kid-1";

    private final ParticipantContextConfig config = mock();
    private final CapturingTokenGenerationService tokenGenerationService = new CapturingTokenGenerationService();
    private final SignatureSecureTokenService service = new SignatureSecureTokenService(tokenGenerationService, config, Clock.systemUTC(), 600);

    @Test
    void createToken_shouldUseKeyNameAndKid_andAddRegisteredClaims() {
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KEY_NAME), eq(null))).thenReturn(KEY_NAME);
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KID), eq(null))).thenReturn(KID);

        var result = service.createToken(PARTICIPANT, Map.of(ISSUER, "did:web:issuer", SUBJECT, "did:web:sub", AUDIENCE, "did:web:aud"), null);

        AbstractResultAssert.assertThat(result).isSucceeded();
        assertThat(tokenGenerationService.privateKeyIds).containsExactly(KEY_NAME);
        var params = tokenGenerationService.captured.get(0);
        assertThat(params.getHeaders()).containsEntry("kid", KID);
        assertThat(params.getClaims()).containsKeys(ISSUER, SUBJECT, AUDIENCE, "iat", "exp", "jti");
    }

    @Test
    void createToken_withBearerScope_shouldNestAccessToken() {
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KEY_NAME), eq(null))).thenReturn(KEY_NAME);
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KID), eq(null))).thenReturn(KID);

        var result = service.createToken(PARTICIPANT, Map.of(ISSUER, "did:web:issuer", SUBJECT, "did:web:sub", AUDIENCE, "did:web:aud"), "org.test.scope");

        AbstractResultAssert.assertThat(result).isSucceeded();
        assertThat(tokenGenerationService.captured).hasSize(2);

        // first generated token is the nested access token: iss -> aud, aud -> sub, plus scope
        var accessToken = tokenGenerationService.captured.get(0);
        assertThat(accessToken.getClaims())
                .containsEntry(AUDIENCE, "did:web:issuer")
                .containsEntry(SUBJECT, "did:web:aud")
                .containsEntry(SCOPE, "org.test.scope");

        // second generated token is the SI token, embedding the access token under the "token" claim
        var siToken = tokenGenerationService.captured.get(1);
        assertThat(siToken.getClaims()).containsKey(PRESENTATION_TOKEN_CLAIM);
    }

    @Test
    void createToken_shouldFail_whenKeyNameMissing() {
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KEY_NAME), eq(null))).thenReturn(null);

        var result = service.createToken(PARTICIPANT, Map.of(ISSUER, "did:web:issuer"), null);

        AbstractResultAssert.assertThat(result).isFailed();
        assertThat(tokenGenerationService.captured).isEmpty();
    }

    @Test
    void createToken_shouldFail_whenKidMissing() {
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KEY_NAME), eq(null))).thenReturn(KEY_NAME);
        when(config.getString(eq(PARTICIPANT), eq(SIGNATURE_KID), eq(null))).thenReturn(null);

        var result = service.createToken(PARTICIPANT, Map.of(ISSUER, "did:web:issuer"), null);

        AbstractResultAssert.assertThat(result).isFailed();
        assertThat(tokenGenerationService.captured).isEmpty();
    }

    /**
     * A {@link TokenGenerationService} that records the calls it receives and materializes the decorated
     * {@link TokenParameters} so the test can assert on the produced claims and headers.
     */
    private static class CapturingTokenGenerationService implements TokenGenerationService {

        private final List<TokenParameters> captured = new ArrayList<>();
        private final List<String> privateKeyIds = new ArrayList<>();

        @Override
        public Result<TokenRepresentation> generate(String participantContextId, String privateKeyId, TokenDecorator... decorators) {
            var builder = TokenParameters.Builder.newInstance();
            for (var decorator : decorators) {
                decorator.decorate(builder);
            }
            captured.add(builder.build());
            privateKeyIds.add(privateKeyId);
            return Result.success(TokenRepresentation.Builder.newInstance().token("token-" + captured.size()).build());
        }
    }
}
