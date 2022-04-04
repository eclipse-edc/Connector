/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;

/**
 * Writes a JWE containing a typed payload. By default the {@link JWEAlgorithm#ECDH_ES_A256KW} in conjunction with the
 * {@link EncryptionMethod#A256GCM} encryption method.
 * <p>
 * <em>Caution! The defaults only work with elliptic curve keys! If you need to use RSA keys, you need to supply a different Algorith and Encryption Method! </em>
 *
 * @see EncryptionMethod
 * @see JWEAlgorithm
 * @see PublicKeyWrapper#encrypter()
 */
public class GenericJweWriter extends AbstractJweWriter<GenericJweWriter> {
    private Object payload;


    @Override
    public String buildJwe() {
        try {
            var jwePayload = new Payload(objectMapper.writeValueAsString(payload));
            var jweAlgorithm = publicKey.jweAlgorithm();
            var encryptionMethod = publicKey.encryptionMethod();
            var jweHeader = new JWEHeader.Builder(jweAlgorithm, encryptionMethod).build();
            var jweObject = new JWEObject(jweHeader, jwePayload);
            jweObject.encrypt(publicKey.encrypter());
            return jweObject.serialize();
        } catch (JsonProcessingException | JOSEException e) {
            throw new EdcException(e);
        }
    }


    public GenericJweWriter payload(Object payload) {
        this.payload = payload;
        return this;
    }
}
