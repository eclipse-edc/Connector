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

package org.eclipse.edc.security.signature.jws2020;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;

import java.text.ParseException;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Utility class that has two basic duties:
 * <ul>
 *     <li>Create cryptographic keys represented in JWK format ({@link JWK}) out of JSON or JSON-like objects</li>
 *     <li>Create {@link JWSSigner}s and {@link JWSVerifier}s for any given key represented in {@link JWK} format.</li>
 * </ul>
 * <p>
 * For this, the well-known <a href="https://connect2id.com/products/nimbus-jose-jwt">Nimbus JOSE+JWT library</a> is used.
 */
public class KeyFactory {

    /**
     * Creates a {@link JWK} out of a map that represents a JSON structure.
     *
     * @param jsonObject The map containing the JSON
     * @return the corresponding key.
     * @throws RuntimeException if the JSON was malformed, or the JWK type was unknown. Typically, this wraps a {@link ParseException}
     */
    public static JWK create(Map<String, Object> jsonObject) {
        if (jsonObject == null) return null;
        try {
            return JWK.parse(jsonObject);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link JWK} out of a JSON string containing the key properties
     *
     * @param json The string containing plain JSON
     * @return the corresponding key.
     * @throws RuntimeException if the JSON was malformed, or the JWK type was unknown. Typically, this wraps a {@link ParseException}
     */
    public static JWK create(String json) {
        if (json == null) return null;
        try {
            return JWK.parse(json);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link JWSVerifier} from the base class {@link JWK}. Currently only supports EC, OKP and RSA keys.
     *
     * @param jwk The {@link JWK} for which the {@link JWSVerifier} is to be created.
     * @return the {@link JWSVerifier}
     * @throws UnsupportedOperationException if the verifier could not be created, in which case the root cause would be {@link JOSEException}
     */
    public static JWSVerifier createVerifier(JWK jwk) {
        Objects.requireNonNull(jwk, "jwk cannot be null");
        var value = jwk.getKeyType().getValue();
        try {
            return switch (value) {
                case "EC" -> new ECDSAVerifier((ECKey) jwk);
                case "OKP" -> new Ed25519Verifier((OctetKeyPair) jwk);
                case "RSA" -> new RSASSAVerifier((RSAKey) jwk);
                default ->
                        throw new UnsupportedOperationException(format("Cannot create JWSVerifier for JWK-type [%s], currently only supporting EC, OKP and RSA", value));
            };
        } catch (JOSEException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    /**
     * Creates a {@link JWSSigner} from the base class {@link JWK}. Currently only supports EC, OKP and RSA keys.
     *
     * @param jwk The {@link JWK} for which the {@link JWSSigner} is to be created.
     * @return the {@link JWSSigner}
     * @throws UnsupportedOperationException if the signer could not be created, in which case the root cause would be {@link JOSEException}
     */
    public static JWSSigner createSigner(JWK jwk) {
        var value = jwk.getKeyType().getValue();
        try {
            return switch (value) {
                case "EC" -> new ECDSASigner((ECKey) jwk);
                case "OKP" -> new Ed25519Signer((OctetKeyPair) jwk);
                case "RSA" -> new RSASSASigner((RSAKey) jwk);
                default ->
                        throw new UnsupportedOperationException(format("Cannot create JWSVerifier for JWK-type [%s], currently only supporting EC, OKP and RSA", value));
            };
        } catch (JOSEException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }
}
