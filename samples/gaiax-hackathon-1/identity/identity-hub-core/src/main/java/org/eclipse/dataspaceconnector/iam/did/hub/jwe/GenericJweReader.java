/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.text.ParseException;
import java.util.Objects;

/**
 * Deserializes a JWE containing a typed payload.
 */
public class GenericJweReader extends AbstractJweReader<GenericJweReader> {

    public <T> T readType(Class<T> payloadType) {
        Objects.requireNonNull(jwe);
        Objects.requireNonNull(privateKey);
        Objects.requireNonNull(mapper);
        try {
            var parsedJwe = JWEObject.parse(jwe);
            parsedJwe.decrypt(new RSADecrypter(privateKey));
            return mapper.readValue(parsedJwe.getPayload().toString(), payloadType);
        } catch (ParseException | JOSEException | JsonProcessingException e) {
            throw new EdcException(e);
        }
    }


}
