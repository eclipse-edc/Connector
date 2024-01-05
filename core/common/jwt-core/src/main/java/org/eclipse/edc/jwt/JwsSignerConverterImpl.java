/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Requirement;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.EdECPrivateKey;
import java.util.Optional;

/**
 * Factory class that converts {@link PrivateKey} objects into their Nimbus-counterparts needed to sign and
 * verify JWTs.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">Defined Algorithm Standard Names</a>
 */
public class JwsSignerConverterImpl implements JwsSignerConverter {


    @Override
    public JWSSigner createSignerFor(PrivateKey key) {
        try {
            return switch (key.getAlgorithm()) {
                case ALGORITHM_EC -> new ECDSASigner((ECPrivateKey) key);
                case ALGORITHM_RSA -> new RSASSASigner(key);
                case ALGORITHM_ECDSA, ALGORITHM_ED25519 -> createOctetKeyPair(key);
                default -> throw new IllegalStateException(notSupportedError(key.getAlgorithm()));
            };
        } catch (JOSEException ex) {
            throw new EdcException(notSupportedError(key.getAlgorithm()), ex);
        }
    }

    @Override
    public JWSAlgorithm getRecommendedAlgorithm(JWSSigner signer) {
        return getWithRequirement(signer, Requirement.REQUIRED)
                .orElseGet(() -> getWithRequirement(signer, Requirement.RECOMMENDED)
                        .orElseGet(() -> getWithRequirement(signer, Requirement.OPTIONAL)
                                .orElse(null)));

    }

    @NotNull
    private Optional<JWSAlgorithm> getWithRequirement(JWSSigner signer, Requirement requirement) {
        return signer.supportedJWSAlgorithms().stream()
                .filter(alg -> alg.getRequirement() == requirement)
                .findFirst();
    }

    private Ed25519Signer createOctetKeyPair(PrivateKey key) throws JOSEException {
        var edKey = (EdECPrivateKey) key;
        var curveName = edKey.getParams().getName();

        if (!ALGORITHM_ED25519.equals(curveName)) {
            throw new IllegalArgumentException("Only the Ed25519 curve is supported in JWSSigners.");
        }
        var curve = Curve.parse(curveName);

        var bytes = edKey.getBytes().orElseThrow(() -> new EdcException("Private key is not willing to disclose its bytes"));

        var urlX = Base64URL.encode(new byte[0]);
        var urlD = Base64URL.encode(bytes);

        // technically, urlX should be the public bytes (i.e. public key), but we don't have that here, and we don't need it.
        // that is because internally, the Ed25519Signer only wraps the Ed25519Sign class from the Tink library, using only the private bytes ("d")
        var octetKeyPair = new OctetKeyPair.Builder(curve, urlX)
                .d(urlD)
                .build();
        return new Ed25519Signer(octetKeyPair);
    }
}
