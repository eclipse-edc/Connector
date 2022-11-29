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
 *
 */

package org.eclipse.edc.connector.dataplane.transfer.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;

import static java.lang.String.join;

/**
 * Tool for converting a PEM-encoded file into a {@link PublicKey}.
 */
public class PublicKeyParser {

    private PublicKeyParser() {
    }

    public static PublicKey from(@NotNull String pem) {
        try {
            var jwk = JWK.parseFromPEMEncodedObjects(pem);
            if (jwk instanceof RSAKey) {
                return jwk.toRSAKey().toPublicKey();
            } else if (jwk instanceof ECKey) {
                return jwk.toECKey().toPublicKey();
            } else {
                throw new EdcException(join("Public key algorithm %s is not supported", jwk.getAlgorithm().toString()));
            }
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}