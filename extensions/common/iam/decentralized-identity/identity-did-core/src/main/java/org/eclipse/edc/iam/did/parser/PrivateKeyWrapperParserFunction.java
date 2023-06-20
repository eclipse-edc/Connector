/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did.parser;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.edc.iam.did.crypto.key.RsaPrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

/**
 * Parse a PEM into a {@link EcPrivateKeyWrapper} or @{@link RsaPrivateKeyWrapper}, depending on the key type.
 */
public class PrivateKeyWrapperParserFunction implements Function<String, PrivateKeyWrapper> {

    @Override
    public PrivateKeyWrapper apply(String encoded) {
        JWK jwk;
        try {
            jwk = JWK.parseFromPEMEncodedObjects(encoded);
        } catch (JOSEException e) {
            throw new EdcException(e);
        }

        if (jwk instanceof RSAKey) {
            return new RsaPrivateKeyWrapper(jwk.toRSAKey());
        } else if (jwk instanceof ECKey) {
            return new EcPrivateKeyWrapper(jwk.toECKey());
        }

        throw new EdcException("Failed to parse private key with type: " + jwk.getKeyType());
    }
}
