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

package org.eclipse.dataspaceconnector.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TokenValidationServiceImpl implements TokenValidationService {

    private final PublicKeyResolver publicKeyResolver;
    private final List<JwtClaimValidationRule> validationRules;

    public TokenValidationServiceImpl(PublicKeyResolver publicKeyResolver, List<JwtClaimValidationRule> validationRules) {
        this.publicKeyResolver = publicKeyResolver;
        this.validationRules = Collections.unmodifiableList(validationRules);
    }

    @Override
    public Result<ClaimToken> validate(@NotNull String token) {
        JWTClaimsSet claimsSet;
        try {
            var jwt = SignedJWT.parse(token);
            var publicKey = publicKeyResolver.resolveKey(jwt.getHeader().getKeyID());
            if (publicKey == null) {
                return Result.failure("Failed to resolve public key with id: " + jwt.getHeader().getKeyID());
            }
            var verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), publicKey);
            if (!jwt.verify(verifier)) {
                return Result.failure("Token verification failed");
            }

            claimsSet = jwt.getJWTClaimsSet();
        } catch (JOSEException e) {
            return Result.failure(e.getMessage());
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }

        var errors = validationRules.stream()
                .map(r -> r.checkRule(claimsSet))
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
    }
}
