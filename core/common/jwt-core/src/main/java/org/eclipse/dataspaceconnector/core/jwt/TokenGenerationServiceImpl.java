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
 *       Microsoft Corporation - Simplified token representation
 *
 */

package org.eclipse.dataspaceconnector.core.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;
import org.eclipse.dataspaceconnector.spi.jwt.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

public class TokenGenerationServiceImpl implements TokenGenerationService {

    private static final String KEY_ALGO_RSA = "RSA";
    private static final String KEY_ALGO_EC = "EC";

    private final JWSSigner tokenSigner;
    private final JwtDecorator baseDecorator;

    public TokenGenerationServiceImpl(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "Private key must not be null");
        this.tokenSigner = createSigner(privateKey);
        JWSAlgorithm jwsAlgorithm;
        if (tokenSigner instanceof ECDSASigner) {
            jwsAlgorithm = JWSAlgorithm.ES256;
        } else {
            jwsAlgorithm = JWSAlgorithm.RS256;
        }
        this.baseDecorator = new BaseDecorator(jwsAlgorithm);
    }

    @Override
    public Result<TokenRepresentation> generate(@NotNull JwtDecorator... decorators) {
        var header = createHeader(decorators);
        var claims = createClaimsSet(decorators);

        var token = new SignedJWT(header, claims);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token: " + e.getMessage());
        }
        return Result.success(TokenRepresentation.Builder.newInstance().token(token.serialize()).build());
    }

    private JWSHeader createHeader(@NotNull JwtDecorator[] decorators) {
        var map = allDecorators(decorators)
                .map(JwtDecorator::headers)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            return JWSHeader.parse(new JSONObject(map));
        } catch (ParseException e) {
            throw new EdcException("Error parsing JWSHeader, this should never happens since the algorithm is always valid", e);
        }
    }

    private JWTClaimsSet createClaimsSet(@NotNull JwtDecorator[] decorators) {
        var builder = new JWTClaimsSet.Builder();

        allDecorators(decorators)
                .map(JwtDecorator::claims)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .forEach(claim -> builder.claim(claim.getKey(), claim.getValue()));

        return builder.build();
    }

    @NotNull
    private Stream<JwtDecorator> allDecorators(@NotNull JwtDecorator[] decorators) {
        return Stream.concat(Stream.of(this.baseDecorator), Arrays.stream(decorators));
    }

    /**
     * Generate a token signer based on a private key.
     */
    private static JWSSigner createSigner(PrivateKey privateKey) {
        switch (privateKey.getAlgorithm()) {
            case KEY_ALGO_EC:
                try {
                    return new ECDSASigner((ECPrivateKey) privateKey);
                } catch (JOSEException e) {
                    throw new EdcException("Failed to generate token signed for EC key: " + e.getMessage());
                }
            case KEY_ALGO_RSA:
                return new RSASSASigner(privateKey);
            default:
                throw new EdcException("Key algorithm not handled: " + privateKey.getAlgorithm());
        }
    }

    /**
     * Base JwtDecorator that provides the algorithm header value
     */
    private static class BaseDecorator implements JwtDecorator {

        private final JWSAlgorithm jwsAlgorithm;

        BaseDecorator(JWSAlgorithm jwsAlgorithm) {
            this.jwsAlgorithm = jwsAlgorithm;
        }

        @Override
        public Map<String, Object> claims() {
            return emptyMap();
        }

        @Override
        public Map<String, Object> headers() {
            return Map.of("alg", jwsAlgorithm.getName());
        }
    }
}
