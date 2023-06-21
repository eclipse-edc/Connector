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
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

/**
 * Parse a PEM into a {@link ECKey}.
 */
public class EcPrivateKeyParserFunction implements Function<String, ECKey> {

    @Override
    public ECKey apply(String encoded) {
        try {
            return JWK.parseFromPEMEncodedObjects(encoded).toECKey();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}
