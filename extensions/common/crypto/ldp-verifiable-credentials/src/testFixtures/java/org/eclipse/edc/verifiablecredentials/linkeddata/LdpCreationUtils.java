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

package org.eclipse.edc.verifiablecredentials.linkeddata;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.ld.signature.key.KeyPair;
import com.apicatalog.vc.issuer.ProofDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.security.signature.jws2020.TestFunctions;
import org.jetbrains.annotations.Nullable;

public class LdpCreationUtils {
    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();
    private static final Jws2020SignatureSuite SUITE = new Jws2020SignatureSuite(MAPPER);

    public static String signDocument(String jsonLdContent, JWK proofKey, ProofDraft proofDraft, @Nullable DocumentLoader testDocLoader) {
        try {
            var jsonLd = MAPPER.readValue(jsonLdContent, JsonObject.class);
            var ldKeypair = TestFunctions.createKeyPair(proofKey);

            return SUITE.createIssuer(ldKeypair)
                    .loader(testDocLoader)
                    .sign(jsonLd, proofDraft)
                    .compacted()
                    .toString();

        } catch (JsonProcessingException | DocumentError | SigningError e) {
            throw new RuntimeException(e);
        }
    }

    public static String signDocument(String jsonLdContent, KeyPair proofKey, ProofDraft proofDraft, @Nullable DocumentLoader testDocLoader) {
        try {
            var jsonLd = MAPPER.readValue(jsonLdContent, JsonObject.class);

            return SUITE.createIssuer(proofKey)
                    .loader(testDocLoader)
                    .sign(jsonLd, proofDraft)
                    .compacted()
                    .toString();
        } catch (JsonProcessingException | DocumentError | SigningError e) {
            throw new RuntimeException(e);
        }
    }
}
