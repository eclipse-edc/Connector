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
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.spi.JwsSignerVerifierFactory;
import org.eclipse.edc.jwt.spi.TokenValidationRule;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.stream.Stream;

public class TokenValidationServiceImpl implements TokenValidationService {

    private final JwsSignerVerifierFactory jwsSignerVerifierFactory;

    public TokenValidationServiceImpl() {
        jwsSignerVerifierFactory = new JwsSignerVerifierFactory();
    }

    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation, PublicKeyResolver publicKeyResolver, TokenValidationRule... rules) {
        var token = tokenRepresentation.getToken();
        var additional = tokenRepresentation.getAdditional();
        try {
            var signedJwt = SignedJWT.parse(token);
            var publicKeyId = signedJwt.getHeader().getKeyID();

            var publicKeyResolutionResult = publicKeyResolver.resolveKey(publicKeyId);

            if (publicKeyResolutionResult.failed()) {
                return publicKeyResolutionResult.mapTo();
            }

            var verifierCreationResult = jwsSignerVerifierFactory.createVerifierFor(publicKeyResolutionResult.getContent());

            if (!signedJwt.verify(verifierCreationResult)) {
                return Result.failure("Token verification failed");
            }

            var tokenBuilder = ClaimToken.Builder.newInstance();
            signedJwt.getJWTClaimsSet().getClaims().entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

            var claimToken = tokenBuilder.build();


            var errors = Stream.of(rules)
                    .map(r -> r.checkRule(claimToken, additional))
                    .reduce(Result::merge)
                    .stream()
                    .filter(AbstractResult::failed)
                    .flatMap(r -> r.getFailureMessages().stream())
                    .toList();


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

}
