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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import org.eclipse.edc.spi.EdcException;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.EdECPrivateKey;
import java.util.List;

/**
 * Factory class that converts {@link PrivateKey} objects into their Nimbus-counterparts needed to sign and
 * verify JWTs.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html">Defined Algorithm Standard Names</a>
 */
public interface JwsSignerConverter {

    String ALGORITHM_RSA = "RSA";
    String ALGORITHM_EC = "EC";
    String ALGORITHM_ECDSA = "EdDSA";
    String ALGORITHM_ED25519 = "Ed25519";
    List<String> SUPPORTED_ALGORITHMS = List.of(ALGORITHM_EC, ALGORITHM_RSA, ALGORITHM_ECDSA, ALGORITHM_ED25519);

    /**
     * Converts a Java {@link PrivateKey} object into a Nimbus {@link JWSSigner} for convenient use.
     * Note that currently only the following key types are supported:
     * <ul>
     *     <li>RSA</li>
     *     <li>EC</li>: {@code key} argument is expected to be instanceof {@link ECPrivateKey}
     *     <li>EdDSA/Ed25519</li>: {@code key} argument ist expected to be {@link EdECPrivateKey}. Both the Sun provider and the {@link org.bouncycastle.jce.provider.BouncyCastleProvider}
     *     are supported.
     * </ul>
     *
     * @param key the private key.
     * @return a {@link JWSSigner}
     * @throws IllegalArgumentException if the Curve of an EdDSA key is not "Ed25519" (x25519 and Ed448 are not supported!)
     * @throws IllegalArgumentException if the key is not in the list of supported algorithms ({@link JwsSignerConverterImpl#SUPPORTED_ALGORITHMS})
     * @throws EdcException             if the {@link PrivateKey} is a EdDSA key and does not disclose its private bytes
     */
    JWSSigner createSignerFor(PrivateKey key);

    /**
     * Attempts to determine the best suitable {@link JWSAlgorithm} for any given signer. Some signers support multiple, in
     * which case the first one marked RECOMMENDED is returned. If none is marked such, the first one is returned.
     *
     * @param signer the {@link JWSSigner}
     * @return the only {@link JWSAlgorithm}, or the one marked RECOMMENDED, or simply the first one. Returns null if no {@link JWSAlgorithm} can be determined.
     */
    JWSAlgorithm getRecommendedAlgorithm(JWSSigner signer);

    default String notSupportedError(String algorithm) {
        return "Could not convert PrivateKey to a JWSSigner, currently only the following types are supported: %s. The specified key was a %s"
                .formatted(String.join(",", SUPPORTED_ALGORITHMS), algorithm);
    }
}
