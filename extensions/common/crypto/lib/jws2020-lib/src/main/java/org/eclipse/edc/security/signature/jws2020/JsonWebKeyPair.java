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

import com.apicatalog.ld.signature.key.KeyPair;
import com.nimbusds.jose.jwk.JWK;

import java.net.URI;

public record JsonWebKeyPair(URI id, URI type, URI controller, JWK keyPair) implements KeyPair {

    @Override
    public byte[] privateKey() {
        return keyPair != null ? serializeKeyPair(keyPair) : null;
    }


    @Override
    public String algorithm() {
        return "JsonWebKey2020";
    }

    @Override
    public byte[] publicKey() {
        return keyPair != null ? serializeKeyPair(keyPair.toPublicJWK()) : null;
    }

    private byte[] serializeKeyPair(JWK keyPair) {
        return keyPair.toJSONString().getBytes();
    }

}
