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
import org.eclipse.edc.jwt.spi.JwsSignerVerifierFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class JwtGenerationService implements TokenGenerationService {
    private final JwsSignerVerifierFactory factory;


    public JwtGenerationService() {
        this.factory = new JwsSignerVerifierFactory();
    }

    @Override
    public Result<TokenRepresentation> generate(Supplier<PrivateKey> privateKeySupplier, @NotNull TokenDecorator... decorators) {

        var privateKey = privateKeySupplier.get();

        var tokenSigner = factory.createSignerFor(privateKey);
        var jwsAlgorithm = factory.getRecommendedAlgorithm(tokenSigner);


        var claims = new HashMap<String, Object>();
        var headers = new HashMap<String, Object>();


        var allDecorators = new ArrayList<>(Arrays.asList(decorators));
        allDecorators.add(new BaseDecorator(jwsAlgorithm));

        allDecorators.forEach(td -> td.decorate(claims, headers));

        var jwsHeader = createHeader(headers);
        var claimsSet = createClaimsSet(claims);

        var token = new SignedJWT(jwsHeader, claimsSet);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token: " + e.getMessage());
        }
        return Result.success(TokenRepresentation.Builder.newInstance().token(token.serialize()).build());
    }

    private JWSHeader createHeader(Map<String, Object> decorators) {
        try {
            return JWSHeader.parse(decorators);
        } catch (ParseException e) {
            throw new EdcException("Error parsing JWSHeader, this should never happens since the algorithm is always valid", e);
        }
    }

    private JWTClaimsSet createClaimsSet(HashMap<String, Object> decorators) {
        var builder = new JWTClaimsSet.Builder();
        decorators.forEach(builder::claim);
        return builder.build();
    }


    /**
     * Base JwtDecorator that provides the algorithm header value
     */
    private record BaseDecorator(JWSAlgorithm jwsAlgorithm) implements TokenDecorator {

        @Override
        public void decorate(Map<String, Object> claims, Map<String, Object> headers) {
            headers.put("alg", jwsAlgorithm.getName());
        }
    }
}
