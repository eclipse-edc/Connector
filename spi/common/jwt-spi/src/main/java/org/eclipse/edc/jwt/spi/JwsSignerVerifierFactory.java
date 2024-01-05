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

package org.eclipse.edc.jwt.spi;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Requirement;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Optional;

/**
 * Factory class that converts {@link PrivateKey} objects into their Nimbus-counterparts needed to sign and
 * verify JWTs.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">Defined Algorithm Standard Names</a>
 */
public class JwsSignerVerifierFactory {

    public static final String ALGORITHM_RSA = "RSA";
    public static final String ALGORITHM_EC = "EC";
    public static final String ALGORITHM_ECDSA = "EdDSA";
    public static final String ALGORITHM_ED25519 = "Ed25519";
    public static final List<String> SUPPORTED_ALGORITHMS = List.of(ALGORITHM_EC, ALGORITHM_RSA, ALGORITHM_ECDSA, ALGORITHM_ED25519);

    private static String notSupportedError(String algorithm) {
        return "Could not convert PrivateKey to a JWSSigner, currently only the following types are supported: %s. The specified key was a %s"
                .formatted(String.join(",", SUPPORTED_ALGORITHMS), algorithm);
    }

    /**
     * Takes a Java {@link PrivateKey} object and creates a corresponding Nimbus {@link JWSSigner} for convenient use with JWTs.
     * Note that currently only the following key types are supported:
     * <ul>
     *     <li>RSA</li>
     *     <li>EC: {@code key} argument is expected to be instanceof {@link ECPrivateKey}</li>
     *     <li>EdDSA/Ed25519: {@code key} argument ist expected to be {@link EdECPrivateKey}. Both the Sun provider and the {@code org.bouncycastle.jce.provider.BouncyCastleProvider}  are supported.</li>
     * </ul>
     *
     * @param key the private key.
     * @return a {@link JWSSigner}
     * @throws IllegalArgumentException if the Curve of an EdDSA key is not "Ed25519" (x25519 and Ed448 are not supported!)
     * @throws IllegalArgumentException if the key is not in the list of supported algorithms ({@link JwsSignerVerifierFactory#SUPPORTED_ALGORITHMS})
     * @throws EdcException             if the {@link PrivateKey} is a EdDSA key and does not disclose its private bytes
     */
    public JWSSigner createSignerFor(PrivateKey key) {
        try {
            return switch (key.getAlgorithm()) {
                case ALGORITHM_EC -> new ECDSASigner((ECPrivateKey) key);
                case ALGORITHM_RSA -> new RSASSASigner(key);
                case ALGORITHM_ECDSA, ALGORITHM_ED25519 -> createOctetKeyPair(key);
                default -> throw new IllegalArgumentException(notSupportedError(key.getAlgorithm()));
            };
        } catch (JOSEException ex) {
            throw new EdcException(notSupportedError(key.getAlgorithm()), ex);
        }
    }

    /**
     * Takes a Java {@link PublicKey} object and creates a corresponding Nimbus {@link JWSVerifier} for convenient use with JWTs.
     * Note that currently only the following key types are supported:
     * <ul>
     *     <li>RSA</li>
     *     <li>EC: {@code key} argument is expected to be instanceof {@link ECPrivateKey}</li>
     *     <li>EdDSA/Ed25519: {@code key} argument ist expected to be {@link EdECPrivateKey}. Both the Sun provider and the {@code org.bouncycastle.jce.provider.BouncyCastleProvider}  are supported.</li>
     * </ul>
     *
     * @param publicKey the public key.
     * @return a {@link JWSSigner}
     * @throws IllegalArgumentException if the Curve of an EdDSA key is not "Ed25519" (x25519 and Ed448 are not supported!)
     * @throws IllegalArgumentException if the key is not in the list of supported algorithms ({@link JwsSignerVerifierFactory#SUPPORTED_ALGORITHMS})
     * @throws EdcException             if the {@link PublicKey} is a EdDSA key and does not disclose its private bytes
     */
    public JWSVerifier createVerifierFor(PublicKey publicKey) {
        try {
            return switch (publicKey.getAlgorithm()) {
                case ALGORITHM_EC -> new ECDSAVerifier((ECPublicKey) publicKey);
                case ALGORITHM_RSA -> new RSASSAVerifier((RSAPublicKey) publicKey);
                case ALGORITHM_ECDSA, ALGORITHM_ED25519 -> createOctetKeyPair(publicKey);
                default -> throw new IllegalArgumentException(notSupportedError(publicKey.getAlgorithm()));
            };
        } catch (JOSEException e) {
            throw new EdcException(notSupportedError(publicKey.getAlgorithm()), e);
        }
    }

    /**
     * Attempts to determine the best suitable {@link JWSAlgorithm} for any given signer. Some signers support multiple, in
     * which case the first one marked RECOMMENDED is returned. If none is marked such, the first one is returned.
     *
     * @param signer the {@link JWSSigner}
     * @return the only {@link JWSAlgorithm}, or the one marked RECOMMENDED, or simply the first one. Returns null if no {@link JWSAlgorithm} can be determined.
     */
    public JWSAlgorithm getRecommendedAlgorithm(JWSSigner signer) {
        return getWithRequirement(signer, Requirement.REQUIRED)
                .orElseGet(() -> getWithRequirement(signer, Requirement.RECOMMENDED)
                        .orElseGet(() -> getWithRequirement(signer, Requirement.OPTIONAL)
                                .orElse(null)));

    }

    private byte[] reverseArray(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            var temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
        return array;
    }

    private Ed25519Verifier createOctetKeyPair(PublicKey publicKey) throws JOSEException {
        var edKey = (EdECPublicKey) publicKey;
        var curveName = edKey.getParams().getName();

        if (!ALGORITHM_ED25519.equals(curveName)) {
            throw new IllegalArgumentException("Only the Ed25519 curve is supported for Ed25519Verifiers.");
        }

        var curve = Curve.parse(curveName);

        var bytes = reverseArray(edKey.getPoint().getY().toByteArray());

        // when the X-coordinate of the curve is odd, we flip the highest-order bit of the first (or last, since we reversed) byte
        if (edKey.getPoint().isXOdd()) {
            byte mask = (byte) 128; // is 1000 0000 binary
            bytes[bytes.length - 1] ^= mask; // XOR means toggle the left-most bit
        }

        var urlX = Base64URL.encode(bytes);
        var okp = new OctetKeyPair.Builder(curve, urlX)
                .build();
        return new Ed25519Verifier(okp);

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
            throw new IllegalArgumentException("Only the Ed25519 curve is supported for Ed25519Signers.");
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
