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

package org.eclipse.dataspaceconnector.common.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
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
        JWTClaimsSet claimsSet;
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

            claimsSet = signedJwt.getJWTClaimsSet();

            var errors = rulesRegistry.getRules().stream()
                    .map(r -> r.checkRule(signedJwt, additional))
                    .filter(Result::failed)
                    .map(Result::getFailureMessages)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            if (!errors.isEmpty()) {
                return Result.failure(errors);
            }

            var tokenBuilder = ClaimToken.Builder.newInstance();
            claimsSet.getClaims().entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), Objects.toString(entry.getValue())))
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

            return Result.success(tokenBuilder.build());

        } catch (JOSEException e) {
            return Result.failure(e.getMessage());
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }
    }

    private Result<JWSVerifier> createVerifier(JWSHeader header, String publicKeyId) {
        var publicKey = publicKeyResolver.resolveKey(publicKeyId);
        if (publicKey == null) {
            return Result.failure("Failed to resolve public key with id: " + publicKeyId);
        }
        try {
            return Result.success(new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey));
        } catch (JOSEException e) {
            return Result.failure("Failed to create verifier");
        }
    }
}
