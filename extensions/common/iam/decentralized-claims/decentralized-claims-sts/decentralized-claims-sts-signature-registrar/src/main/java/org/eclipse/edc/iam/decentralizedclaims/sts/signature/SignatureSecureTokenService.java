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

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.SignatureService;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.decentralizedclaims.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of a {@link SecureTokenService} that creates a self-signed ID token ("SI token") in-process, but
 * delegates the signing operation to a {@link SignatureService}, so the private key never leaves the signing service.
 * <p>
 * The signing key name (used as {@code privateKeyId}) and the JWT {@code kid} header are resolved per participant
 * context from the participant configuration ({@code edc.iam.sts.signature.keyname} and {@code edc.iam.sts.signature.kid}).
 *
 * @see org.eclipse.edc.spi.security.SignatureService
 */
public class SignatureSecureTokenService implements SecureTokenService {

    public static final String SIGNATURE_KEY_NAME = "edc.iam.sts.signature.keyname";
    public static final String SIGNATURE_KID = "edc.iam.sts.signature.kid";

    private static final List<String> ACCESS_TOKEN_INHERITED_CLAIMS = List.of(ISSUER);

    private final TokenGenerationService tokenGenerationService;
    private final ParticipantContextConfig participantContextConfig;
    private final Clock clock;
    private final long validity;

    public SignatureSecureTokenService(TokenGenerationService tokenGenerationService, ParticipantContextConfig participantContextConfig, Clock clock, long validity) {
        this.tokenGenerationService = tokenGenerationService;
        this.participantContextConfig = participantContextConfig;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public Result<TokenRepresentation> createToken(String participantContextId, Map<String, Object> claims, @Nullable String bearerAccessScope) {
        var keyName = participantContextConfig.getString(participantContextId, SIGNATURE_KEY_NAME, null);
        if (keyName == null) {
            return failure("Missing '%s' configuration for participant context '%s'".formatted(SIGNATURE_KEY_NAME, participantContextId));
        }
        var kid = participantContextConfig.getString(participantContextId, SIGNATURE_KID, null);
        if (kid == null) {
            return failure("Missing '%s' configuration for participant context '%s'".formatted(SIGNATURE_KID, participantContextId));
        }

        var selfIssuedClaims = new HashMap<>(claims);
        return ofNullable(bearerAccessScope)
                .map(scope -> createAndAcceptAccessToken(participantContextId, keyName, kid, claims, scope, selfIssuedClaims::put))
                .orElse(success())
                .compose(v -> tokenGenerationService.generate(participantContextId, keyName,
                        new KeyIdDecorator(kid), new SelfIssuedTokenDecorator(selfIssuedClaims, clock, validity)));
    }

    private Result<Void> createAndAcceptAccessToken(String participantContextId, String keyName, String kid, Map<String, Object> claims, String scope, BiConsumer<String, Object> consumer) {
        return createAccessToken(participantContextId, keyName, kid, claims, scope)
                .compose(tokenRepresentation -> success(tokenRepresentation.getToken()))
                .onSuccess(withClaim(PRESENTATION_TOKEN_CLAIM, consumer))
                .mapEmpty();
    }

    private Result<TokenRepresentation> createAccessToken(String participantContextId, String keyName, String kid, Map<String, Object> claims, String bearerAccessScope) {
        var accessTokenClaims = new HashMap<String, Object>(accessTokenInheritedClaims(claims));
        accessTokenClaims.put(SCOPE, bearerAccessScope);

        return addClaim(claims, ISSUER, withClaim(AUDIENCE, accessTokenClaims::put))
                .compose(v -> addClaim(claims, AUDIENCE, withClaim(SUBJECT, accessTokenClaims::put)))
                .compose(v -> tokenGenerationService.generate(participantContextId, keyName,
                        new KeyIdDecorator(kid), new SelfIssuedTokenDecorator(accessTokenClaims, clock, validity)));
    }

    private Result<Void> addClaim(Map<String, Object> claims, String claim, Consumer<Object> consumer) {
        var claimValue = claims.get(claim);
        if (claimValue != null) {
            consumer.accept(claimValue);
            return success();
        } else {
            return failure(format("Missing %s in the input claims", claim));
        }
    }

    private <T> Consumer<T> withClaim(String key, BiConsumer<String, ? super T> consumer) {
        return value -> consumer.accept(key, value);
    }

    private Map<String, Object> accessTokenInheritedClaims(Map<String, Object> claims) {
        return claims.entrySet().stream()
                .filter(entry -> ACCESS_TOKEN_INHERITED_CLAIMS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
