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

import com.apicatalog.ld.schema.LdSchema;
import com.apicatalog.ld.schema.LdTerm;
import com.apicatalog.ld.signature.CryptoSuite;
import com.apicatalog.ld.signature.SignatureSuite;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;

import java.net.URI;

/**
 * {@link SignatureSuite} that provides cryptographic facilities and a key schema for <a href="https://www.w3.org/community/reports/credentials/CG-FINAL-lds-jws2020-20220721/#test-vectors">Json Web Signature 2020</a>.
 */
public final class JwsSignature2020Suite implements SignatureSuite {
    static final URI CONTEXT = URI.create("https://w3id.org/security/suites/jws-2020/v1");
    private final Jws2020CryptoSuite cryptoSuite;
    private final ObjectMapper mapper;

    /**
     * Creates a new {@link JwsSignature2020Suite} using an object mapper. That mapper is needed because parts of the schema are plain JSON.
     *
     * @param mapper a JSON-aware {@link ObjectMapper}, e.g. using {@link  JacksonJsonLd#createObjectMapper()}
     * @see Jws2020Schema
     */
    public JwsSignature2020Suite(ObjectMapper mapper) {
        this.mapper = mapper;
        cryptoSuite = new Jws2020CryptoSuite(LdTerm.ID);
    }

    @Override
    public LdTerm getId() {
        return Jws2020Schema.JSON_WEB_SIGNATURE_TYPE;
    }

    @Override
    public URI getContext() {
        return CONTEXT;
    }

    @Override
    public LdSchema getSchema() {
        return Jws2020Schema.create(mapper);
    }

    @Override
    public CryptoSuite getCryptoSuite() {
        return cryptoSuite;
    }

    @Override
    public JwsSignatureProofOptions createOptions() {
        return new JwsSignatureProofOptions(this);
    }
}
