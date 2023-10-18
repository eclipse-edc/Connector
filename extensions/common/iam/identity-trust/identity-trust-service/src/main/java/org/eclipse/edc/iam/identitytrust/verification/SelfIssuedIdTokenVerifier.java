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

package org.eclipse.edc.iam.identitytrust.verification;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.crypto.JwtUtils;
import org.eclipse.edc.iam.did.crypto.key.KeyConverter;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

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
 * This is done by the {@link org.eclipse.edc.iam.identitytrust.validation.SelfIssuedIdTokenValidator}.
 *
 * @see org.eclipse.edc.iam.identitytrust.validation.SelfIssuedIdTokenValidator For SI Token validation.
 */
public class SelfIssuedIdTokenVerifier implements JwtVerifier {
    private final DidResolverRegistry resolverRegistry;

    public SelfIssuedIdTokenVerifier(DidResolverRegistry resolverRegistry) {
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    public Result<Void> verify(String serializedJwt, String audience) {

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(serializedJwt);
            var didResult = resolverRegistry.resolve(jwt.getJWTClaimsSet().getIssuer());
            if (didResult.failed()) {
                return Result.failure("Unable to resolve DID: %s".formatted(didResult.getFailureDetail()));
            }

            // this will return the _first_ public key entry
            var keyId = jwt.getHeader().getKeyID();

            //either get the first verification method, or the one specified by the key id
            var publicKey = Optional.ofNullable(keyId)
                    .map(kid -> getVerificationMethod(didResult.getContent(), kid))
                    .orElseGet(() -> firstVerificationMethod(didResult.getContent()));

            if (publicKey.isEmpty()) {
                return Result.failure("Public Key not found in DID Document.");
            }

            //convert the POJO into a usable PK-wrapper:
            var publicKeyJwk = publicKey.get().getPublicKeyJwk();
            var publicKeyWrapperResult = KeyConverter.toPublicKeyWrapper(publicKeyJwk, publicKey.get().getId());
            if (publicKeyWrapperResult.failed()) {
                return publicKeyWrapperResult.mapTo();
            }

            var verified = JwtUtils.verify(jwt, publicKeyWrapperResult.getContent(), audience);
            if (verified.failed()) {
                return Result.failure("Token could not be verified: %s".formatted(verified.getFailureDetail()));
            }
            return Result.success();
        } catch (ParseException e) {
            return Result.failure("Error parsing JWT");
        }
    }

    private Optional<VerificationMethod> getVerificationMethod(DidDocument content, String kid) {
        return content.getVerificationMethod().stream().filter(vm -> vm.getId().equals(kid))
                .findFirst();
    }

    @NotNull
    private Optional<VerificationMethod> firstVerificationMethod(DidDocument did) {
        return did.getVerificationMethod().stream()
                .filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType()))
                .findFirst();
    }
}
