/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Collection;
import java.util.stream.Collectors;

public class TokenValidationServiceImpl implements TokenValidationService {

    private final PublicKeyResolver publicKeyResolver;
    private final TokenValidationRulesRegistry rulesRegistry;

    public TokenValidationServiceImpl(PublicKeyResolver publicKeyResolver, TokenValidationRulesRegistry rulesRegistry) {
        this.publicKeyResolver = publicKeyResolver;
        this.rulesRegistry = rulesRegistry;
    }

    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation) {
        var token = tokenRepresentation.getToken();
        var additional = tokenRepresentation.getAdditional();
        try {
            var signedJwt = SignedJWT.parse(token);
            var publicKeyId = signedJwt.getHeader().getKeyID();
            var verifierCreationResult = createVerifier(signedJwt.getHeader(), publicKeyId);

            if (verifierCreationResult.failed()) {
                return Result.failure(verifierCreationResult.getFailureMessages());
            }

            if (!signedJwt.verify(verifierCreationResult.getContent())) {
                return Result.failure("Token verification failed");
            }

            var tokenBuilder = ClaimToken.Builder.newInstance();
            signedJwt.getJWTClaimsSet().getClaims().entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

            var claimToken = tokenBuilder.build();

            var errors = rulesRegistry.getRules().stream()
                    .map(r -> r.checkRule(claimToken, additional))
                    .filter(Result::failed)
                    .map(Result::getFailureMessages)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            if (!errors.isEmpty()) {
                return Result.failure(errors);
            }

            return Result.success(claimToken);

        } catch (JOSEException e) {
            return Result.failure(e.getMessage());
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }
    }

    private Result<JWSVerifier> createVerifier(JWSHeader header, String publicKeyId) {
        var publicKey = publicKeyResolver.resolveKey(publicKeyId);
        if (publicKey.failed()) {
            return Result.failure("Failed to resolve public key with id: %s, Error: %s".formatted(publicKeyId, publicKey.getFailureDetail()));
        }
        try {
            return Result.success(new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey.getContent()));
        } catch (JOSEException e) {
            return Result.failure("Failed to create verifier: " + e.getMessage());
        }
    }
}
