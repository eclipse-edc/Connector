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

import com.apicatalog.ld.schema.LdProperty;
import com.apicatalog.ld.schema.LdSchema;
import com.apicatalog.ld.schema.LdTerm;
import com.apicatalog.vc.VcTag;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

import static com.apicatalog.ld.schema.LdSchema.id;
import static com.apicatalog.ld.schema.LdSchema.link;
import static com.apicatalog.ld.schema.LdSchema.object;
import static com.apicatalog.ld.schema.LdSchema.property;
import static com.apicatalog.ld.schema.LdSchema.string;
import static com.apicatalog.ld.schema.LdSchema.type;
import static com.apicatalog.ld.schema.LdSchema.xsdDateTime;
import static com.apicatalog.vc.VcSchema.proof;
import static com.apicatalog.vc.VcSchema.verificationMethod;
import static com.apicatalog.vc.VcVocab.SECURITY_VOCAB;
import static com.apicatalog.vc.integrity.DataIntegrity.CHALLENGE;
import static com.apicatalog.vc.integrity.DataIntegrity.CREATED;
import static com.apicatalog.vc.integrity.DataIntegrity.DOMAIN;
import static com.apicatalog.vc.integrity.DataIntegrity.PURPOSE;
import static com.apicatalog.vc.integrity.DataIntegrity.VERIFICATION_METHOD;

/**
 * Internal class that encapsulates all JSON schemas that are relevant for JWS2020, such as the structure of the verification method
 */
class Jws2020Schema {
    public static final LdTerm JSON_WEB_KEY_TYPE = LdTerm.create("JsonWebKey2020", SECURITY_VOCAB);
    public static final LdTerm JSON_WEB_SIGNATURE_TYPE = LdTerm.create("JsonWebSignature2020", SECURITY_VOCAB);
    public static final LdTerm JWS = LdTerm.create("jws", SECURITY_VOCAB);
    public static final LdTerm CONTROLLER = LdTerm.create("controller", SECURITY_VOCAB);
    public static final LdTerm JWK_PRIVATE_KEY = LdTerm.create("privateKeyJwk", SECURITY_VOCAB);
    public static final LdTerm JWK_PUBLIC_KEY = LdTerm.create("publicKeyJwk", SECURITY_VOCAB);


    /**
     * Creates an {@link LdSchema} for the verification method object of JWS2020, which looks roughly like this:
     * <pre>
     *     "verificationMethod": [
     *       {
     *         "id": "#ovsDKYBjFemIy8DVhc-w2LSi8CvXMw2AYDzHj04yxkc",
     *         "type": "JsonWebKey2020",
     *         "controller": "https://example.com/issuer/123",
     *         "publicKeyJwk": {
     *           "kty": "OKP",
     *           "crv": "Ed25519",
     *           "x": "CV-aGlld3nVdgnhoZK0D36Wk-9aIMlZjZOK2XhPMnkQ"
     *         }
     *       }
     *     ],
     * </pre>
     *
     * @param mapper The object mapper that is used to deserialize the {@code publicKeyJwk} part.
     * @return The {@link LdSchema} that represents the above structure. Never null.
     */
    public static LdSchema create(ObjectMapper mapper) {
        return proof(
                type(JSON_WEB_SIGNATURE_TYPE).required(),
                property(CREATED, xsdDateTime())
                        .test(created -> Instant.now().isAfter(created))
                        .optional(),
                property(CONTROLLER, link()),
                property(PURPOSE, link()).required().test(uri -> uri.toString().equals("https://w3id.org/security#assertionMethod")),
                verificationMethod(VERIFICATION_METHOD, getVerificationMethod(mapper).map(new JwkAdapter())).required(),
                property(DOMAIN, string())
                        .test((domain, params) -> !params.containsKey(DOMAIN.name()) || params.get(DOMAIN.name()).equals(domain)),
                property(CHALLENGE, string()),
                property(JWS, new ByteArrayAdapter(), VcTag.ProofValue.name())
        );
    }

    private static LdSchema getVerificationMethod(ObjectMapper mapper) {
        return jsonWebKeySchema(mapper);
    }

    private static LdSchema jsonWebKeySchema(ObjectMapper mapper) {
        return new LdSchema(
                object(
                        id().required(),
                        type(JSON_WEB_KEY_TYPE),
                        property(CONTROLLER, link()),
                        jwkPublicKey(mapper)
                ));
    }

    private static LdProperty<?> jwkPublicKey(ObjectMapper mapper) {
        return property(JWK_PUBLIC_KEY, new JsonAdapter(mapper));
    }

    private static LdProperty<?> jwkPrivateKey(ObjectMapper mapper) {
        return property(JWK_PRIVATE_KEY, new JsonAdapter(mapper));
    }
}
