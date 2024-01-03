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

package org.eclipse.edc.connector.core.security.keyparsers;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This parser can resolve private keys that are encoded in PEM format.
 */
public class PemParser implements KeyParser {
    // matches based on the "-----BEGIN XYZ-----" and "-----END XYZ-----" tokens
    // captures 3 groups: the BEGIN token, the base64 text and the END token
    // to parse the PEM, use the second group
    private static final Pattern PEM_FORMAT_REGEX = Pattern.compile("(-----BEGIN\\s.*-----)\\n((?s:.*))(-----END\\s.*-----)");
    private final JcaPEMKeyConverter pemConverter = new JcaPEMKeyConverter();
    private final Monitor monitor;

    public PemParser(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(String encoded) {
        return PEM_FORMAT_REGEX.matcher(encoded).find();
    }

    @Override
    public Result<PrivateKey> parse(String encoded) {

        var matcher = PEM_FORMAT_REGEX.matcher(encoded);
        if (!matcher.find()) {
            return Result.failure("The given input is not valid PEM.");
        }
        var keypair = parseKeys(encoded);

        if (keypair.succeeded()) {
            return keypair.getContent()
                    .stream()
                    .map(KeyPair::getPrivate)
                    .filter(Objects::nonNull) // PEM strings that only contain public keys would get eliminated here
                    .findFirst()
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("PEM-encoded structure did not contain a private key."));
        }

        return keypair.mapTo();
    }

    /**
     * Parses one or more PEM-encoded certificates, public and / or private
     * keys. The input is assumed to be not password-protected.
     * <p>
     * <em>Note: this class was duplicated from the Nimbus code base, it originally is implemented in PEMEncodedKeyParser.java</em>
     *
     * @param pemEncodedKeys String of one or more PEM-encoded keys.
     * @return The found keys or a failure
     */
    private Result<List<KeyPair>> parseKeys(String pemEncodedKeys) {

        // Strips the "---- {BEGIN,END} {CERTIFICATE,PUBLIC/PRIVATE KEY} -----"-like header and footer lines,
        // base64-decodes the body,
        // then uses the proper key specification format to turn it into a JCA Key instance
        final Reader pemReader = new StringReader(pemEncodedKeys);
        var parser = new PEMParser(pemReader);
        var keys = new ArrayList<KeyPair>();

        try {
            Object pemObj;
            do {
                pemObj = parser.readObject();
                if (pemObj instanceof SubjectPublicKeyInfo) { // if public key, use as-is
                    keys.add(toKeyPair((SubjectPublicKeyInfo) pemObj));
                } else if (pemObj instanceof X509CertificateHolder) { // if it's a certificate, use the public key which is signed
                    keys.add(toKeyPair((X509CertificateHolder) pemObj));
                } else if (pemObj instanceof PEMKeyPair) { // if EC private key given, it arrives here as a keypair
                    keys.add(toKeyPair((PEMKeyPair) pemObj));
                } else if (pemObj instanceof PrivateKeyInfo) { // if (RSA) private key given, return it
                    keys.add(toKeyPair((PrivateKeyInfo) pemObj));
                }
            } while (pemObj != null);

            return Result.success(keys);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            monitor.warning("Error parsing PEM-encoded private key", e);
            return Result.failure("Error parsing PEM-encoded private key: " + e.getMessage());
        }
    }

    private KeyPair toKeyPair(final SubjectPublicKeyInfo spki) throws PEMException {
        return new KeyPair(pemConverter.getPublicKey(spki), null);
    }

    private KeyPair toKeyPair(final X509CertificateHolder pemObj) throws PEMException {
        var spki = pemObj.getSubjectPublicKeyInfo();
        return new KeyPair(pemConverter.getPublicKey(spki), null);
    }

    private KeyPair toKeyPair(final PEMKeyPair pair) throws PEMException {
        return pemConverter.getKeyPair(pair);
    }

    private KeyPair toKeyPair(final PrivateKeyInfo pki) throws PEMException, NoSuchAlgorithmException, InvalidKeySpecException {
        var privateKey = pemConverter.getPrivateKey(pki);

        // If it's RSA, we can use the modulus and public exponents as BigIntegers to create a public key
        if (privateKey instanceof RSAPrivateCrtKey) {
            var publicKeySpec =
                    new RSAPublicKeySpec(((RSAPrivateCrtKey) privateKey).getModulus(),
                            ((RSAPrivateCrtKey) privateKey).getPublicExponent());

            var keyFactory = KeyFactory.getInstance("RSA");
            var publicKey = keyFactory.generatePublic(publicKeySpec);
            return new KeyPair(publicKey, privateKey);
        }

        // If was a private EC key, it would already have been received as a PEMKeyPair
        return new KeyPair(null, privateKey);
    }
}
