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

package org.eclipse.edc.verifiablecredentials.verfiablecredentials;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.ld.signature.key.KeyPair;
import com.apicatalog.ld.signature.proof.ProofOptions;
import com.apicatalog.vc.Vc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.security.signature.jws2020.IssuerCompatibility;
import org.eclipse.edc.security.signature.jws2020.TestFunctions;
import org.jetbrains.annotations.Nullable;

public class LdpCreationUtils {
    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();

    public static String signDocument(String jsonLdContent, JWK proofKey, ProofOptions proofOptions, @Nullable DocumentLoader testDocLoader) {
        try {
            var jsonLd = MAPPER.readValue(jsonLdContent, JsonObject.class);
            var ldKeypair = TestFunctions.createKeyPair(proofKey);
            var issuer = Vc.sign(jsonLd, ldKeypair, proofOptions).loader(testDocLoader);
            return IssuerCompatibility.compact(issuer).toString();
        } catch (JsonProcessingException | DocumentError | SigningError e) {
            throw new RuntimeException(e);
        }
    }

    public static String signDocument(String jsonLdContent, KeyPair proofKey, ProofOptions proofOptions, @Nullable DocumentLoader testDocLoader) {
        try {
            var jsonLd = MAPPER.readValue(jsonLdContent, JsonObject.class);
            var issuer = Vc.sign(jsonLd, proofKey, proofOptions).loader(testDocLoader);
            return IssuerCompatibility.compact(issuer).toString();
        } catch (JsonProcessingException | DocumentError | SigningError e) {
            throw new RuntimeException(e);
        }
    }
}
