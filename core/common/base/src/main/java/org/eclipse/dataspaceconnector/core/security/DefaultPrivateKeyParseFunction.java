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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.util.Optional;
import java.util.function.Function;

/**
 * Default parse function for {@link PrivateKey}.
 */
public class DefaultPrivateKeyParseFunction implements Function<String, PrivateKey> {

    @Override
    public PrivateKey apply(String encoded) {
        var pemParser = new PEMParser(new StringReader(encoded));
        var converter = new JcaPEMKeyConverter();
        try {
            var o = pemParser.readObject();
            if (o instanceof PEMKeyPair) {
                var keyPair = (PEMKeyPair) o;
                return converter.getKeyPair(keyPair).getPrivate();
            } else if (o instanceof PrivateKeyInfo) {
                var privateKeyInfo = PrivateKeyInfo.getInstance(o);
                return converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new EdcException(Optional.ofNullable(o)
                        .map(obj -> "Invalid object type: " + obj.getClass())
                        .orElse("Object cannot be null"));
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
