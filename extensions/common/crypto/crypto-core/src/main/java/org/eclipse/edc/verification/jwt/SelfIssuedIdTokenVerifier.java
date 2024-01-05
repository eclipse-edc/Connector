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

package org.eclipse.edc.verification.jwt;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.crypto.JwtUtils;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;

import java.text.ParseException;
import java.util.Optional;

/**
 * Performs cryptographic (and some structural) verification of a self-issued ID token. To that end, the issuer of the token
 * ({@code iss} claim) is presumed to be a Decentralized Identifier (<a href="https://www.w3.org/TR/did-core/">DID</a>).
 * <p>
 * If the JWT contains in its header a {@code kid} field identifying the public key that was used for signing, the DID is
 * <strong>expected</strong> to have a <a href="https://www.w3.org/TR/did-core/#verification-methods">verificationMethod</a>
 * with that same ID. If no such verification method is found, {@link Result#failure(String)} is returned.
 * <p>
 * If no such {@code kid} header is present, then the <em>first</em> verification method is used.
 * <p>
 * Please note that <strong>no structural</strong> validation is done beyond the very basics (must have iss and aud claim).
 * This is done by the {@link SelfIssuedIdTokenVerifier}.
 */
public class SelfIssuedIdTokenVerifier implements JwtVerifier {
    private final PublicKeyResolver publicKeyResolver;

    public SelfIssuedIdTokenVerifier(PublicKeyResolver publicKeyResolver) {
        this.publicKeyResolver = publicKeyResolver;
    }

    @Override
    public Result<Void> verify(String serializedJwt, String audience) {

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(serializedJwt);

            var issuer = jwt.getJWTClaimsSet().getIssuer();
            var keyId = Optional.of(jwt.getHeader().getKeyID()).map(kid -> issuer + "#" + kid).orElseGet(() -> issuer);

            var publicKeyResult = publicKeyResolver.resolveKey(keyId);
            if (publicKeyResult.failed()) {
                return publicKeyResult.mapTo();
            }

            var verified = JwtUtils.verify(jwt, publicKeyResult.getContent(), audience);
            if (verified.failed()) {
                return Result.failure("Token could not be verified: %s".formatted(verified.getFailureDetail()));
            }
            return Result.success();
        } catch (ParseException e) {
            return Result.failure("Error parsing JWT");
        }
    }

}
