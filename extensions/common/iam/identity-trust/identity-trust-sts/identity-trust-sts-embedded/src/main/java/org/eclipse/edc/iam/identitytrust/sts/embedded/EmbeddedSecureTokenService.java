/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of a {@link SecureTokenService}, that is capable of creating a self-signed ID token ("SI token") completely in-process.
 * To that end, it makes use of the <a href="https://connect2id.com/products/nimbus-jose-jwt">Nimbus JOSE/JWT library</a>.<br/>
 * As a recommendation, the private key it uses should not be used for anything else.
 */
public class EmbeddedSecureTokenService implements SecureTokenService {

    private static final List<String> ACCESS_TOKEN_INHERITED_CLAIMS = List.of(ISSUER);
    private final TokenGenerationService tokenGenerationService;
    private final Supplier<String> privateKeyIdSupplier;
    private final Supplier<String> publicKeyIdSupplier;
    private final Clock clock;
    private final long validity;
    private final JtiValidationStore jtiValidationStore;

    public EmbeddedSecureTokenService(TokenGenerationService tokenGenerationService, Supplier<String> privateKeyIdSupplier, Supplier<String> publicKeyIdSupplier, Clock clock, long validity, JtiValidationStore jtiValidationStore) {
        this.tokenGenerationService = tokenGenerationService;
        this.privateKeyIdSupplier = privateKeyIdSupplier;
        this.publicKeyIdSupplier = publicKeyIdSupplier;
        this.clock = clock;
        this.validity = validity;
        this.jtiValidationStore = jtiValidationStore;
    }

    @Override
    public Result<TokenRepresentation> createToken(Map<String, String> claims, @Nullable String bearerAccessScope) {
        var selfIssuedClaims = new HashMap<>(claims);
        return ofNullable(bearerAccessScope)
                .map(scope -> createAndAcceptAccessToken(claims, scope, selfIssuedClaims::put))
                .orElse(success())
                .compose(v -> recordToken(claims))
                .compose(v -> {
                    var keyIdDecorator = new KeyIdDecorator(publicKeyIdSupplier.get());
                    return tokenGenerationService.generate(privateKeyIdSupplier.get(), keyIdDecorator, new SelfIssuedTokenDecorator(selfIssuedClaims, clock, validity));
                });
    }

    private Result<Void> recordToken(Map<String, String> claims) {
        var jti = claims.get(JwtRegisteredClaimNames.JWT_ID);
        if (jti != null) {
            var exp = claims.get(JwtRegisteredClaimNames.EXPIRATION_TIME);
            var expTime = ofNullable(exp).map(Long::parseLong).orElse(null);
            var storeResult = jtiValidationStore.storeEntry(new JtiValidationEntry(jti, expTime));
            return storeResult.succeeded()
                    ? Result.success()
                    : failure("error storing JTI for later validation: %s".formatted(storeResult.getFailureDetail()));
        }
        return Result.success();
    }

    private Result<Void> createAndAcceptAccessToken(Map<String, String> claims, String scope, BiConsumer<String, String> consumer) {
        return createAccessToken(claims, scope)
                .compose(tokenRepresentation -> success(tokenRepresentation.getToken()))
                .onSuccess(withClaim(PRESENTATION_TOKEN_CLAIM, consumer))
                .mapEmpty();
    }

    private Result<TokenRepresentation> createAccessToken(Map<String, String> claims, String bearerAccessScope) {
        var accessTokenClaims = new HashMap<>(accessTokenInheritedClaims(claims));
        accessTokenClaims.put(SCOPE, bearerAccessScope);
        return addClaim(claims, ISSUER, withClaim(AUDIENCE, accessTokenClaims::put))
                .compose(v -> addClaim(claims, AUDIENCE, withClaim(SUBJECT, accessTokenClaims::put)))
                .compose(v -> {
                    var keyIdDecorator = new KeyIdDecorator(publicKeyIdSupplier.get());
                    return tokenGenerationService.generate(privateKeyIdSupplier.get(), keyIdDecorator, new SelfIssuedTokenDecorator(accessTokenClaims, clock, validity));
                });

    }

    private Result<Void> addClaim(Map<String, String> claims, String claim, Consumer<String> consumer) {
        var claimValue = claims.get(claim);
        if (claimValue != null) {
            consumer.accept(claimValue);
            return success();
        } else {
            return failure(format("Missing %s in the input claims", claim));
        }
    }

    private Consumer<String> withClaim(String key, BiConsumer<String, String> consumer) {
        return (value) -> consumer.accept(key, value);
    }

    private Map<String, String> accessTokenInheritedClaims(Map<String, String> claims) {
        return claims.entrySet().stream()
                .filter(entry -> ACCESS_TOKEN_INHERITED_CLAIMS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
