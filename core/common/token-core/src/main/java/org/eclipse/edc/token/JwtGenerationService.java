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

package org.eclipse.edc.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class JwtGenerationService implements TokenGenerationService {

    private final JwsSignerProvider jwsGeneratorFunction;

    public JwtGenerationService(JwsSignerProvider jwsSignerProvider) {

        this.jwsGeneratorFunction = jwsSignerProvider;
    }

    @Override
    public Result<TokenRepresentation> generate(String privateKeyId, @NotNull TokenDecorator... decorators) {

        var tokenSignerResult = jwsGeneratorFunction.createJwsSigner(privateKeyId);
        if (tokenSignerResult.failed()) {
            return Result.failure("JWSSigner cannot be generated for private key '%s': %s".formatted(privateKeyId, tokenSignerResult.getFailureDetail()));
        }

        var tokenSigner = tokenSignerResult.getContent();
        var jwsAlgorithm = CryptoConverter.getRecommendedAlgorithm(tokenSigner);

        var bldr = TokenParameters.Builder.newInstance();
        var allDecorators = new ArrayList<>(Arrays.asList(decorators));
        allDecorators.add(new BaseDecorator(jwsAlgorithm));

        allDecorators.forEach(td -> td.decorate(bldr));
        var tokenParams = bldr.build();
        var jwsHeader = createHeader(tokenParams.getHeaders());
        var claimsSet = createClaimsSet(tokenParams.getClaims());

        var token = new SignedJWT(jwsHeader, claimsSet);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token: " + e.getMessage());
        }
        return Result.success(TokenRepresentation.Builder.newInstance().token(token.serialize()).build());
    }

    private JWSHeader createHeader(Map<String, Object> headers) {
        try {
            return JWSHeader.parse(headers);
        } catch (ParseException e) {
            throw new EdcException("Error parsing JWSHeader, this should never happens since the algorithm is always valid", e);
        }
    }

    private JWTClaimsSet createClaimsSet(Map<String, Object> claims) {
        var builder = new JWTClaimsSet.Builder();
        claims.forEach(builder::claim);
        return builder.build();
    }


    /**
     * Base JwtDecorator that provides the algorithm header value
     */
    private record BaseDecorator(JWSAlgorithm jwsAlgorithm) implements TokenDecorator {

        @Override
        public TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters) {
            return tokenParameters.header("alg", jwsAlgorithm.getName());
        }
    }
}
