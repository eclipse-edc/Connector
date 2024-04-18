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

import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.ld.signature.key.KeyPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

public class TestFunctions {

    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();

    public static JsonObject readResourceAsJson(String name) {
        try {
            return MAPPER.readValue(getResourceFileContentAsString(name), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static VerificationMethod createKeyPair(JWK jwk, String id) {
        var type = URI.create("https://w3id.org/security#JsonWebKey2020");
        return new JsonWebKeyPair(URI.create(id), type, null, jwk);
    }

    public static KeyPair createKeyPair(JWK jwk) {
        var id = URI.create("https://org.eclipse.edc/keys/" + UUID.randomUUID());
        var type = URI.create("https://w3id.org/security#JsonWebKey2020");
        return new JsonWebKeyPair(id, type, URI.create("did:key:zQ3shP2mWsZYWgvgM11nenXRTx9L1yiJKmkf9dfX7NaMKb1pX"), jwk);
    }
}
