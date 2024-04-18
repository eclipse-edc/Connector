/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import com.apicatalog.ld.node.LdNodeBuilder;
import com.apicatalog.ld.signature.CryptoSuite;
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.ModelVersion;
import com.apicatalog.vc.integrity.DataIntegrityVocab;
import com.apicatalog.vc.issuer.ProofDraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite.PROOF_VALUE_TERM;

public class Jws2020ProofDraft extends ProofDraft {

    private final Instant created;
    private final URI proofPurpose;
    private ObjectMapper mapper;

    private Jws2020ProofDraft(CryptoSuite crypto, VerificationMethod method, Instant created, URI proofPurpose) {
        super(crypto, method);
        this.created = created;
        this.proofPurpose = proofPurpose;
    }

    public static JsonObject signed(JsonObject unsigned, JsonValue proofValue) {
        return LdNodeBuilder.of(unsigned).set(PROOF_VALUE_TERM).value(proofValue).build();
    }

    @Override
    public Collection<String> context(ModelVersion model) {
        return List.of(Jws2020SignatureSuite.CONTEXT);

    }

    @Override
    public JsonObject unsigned() {
        var builder = new LdNodeBuilder();
        super.unsigned(builder, new Jwk2020KeyAdapter(mapper));

        builder.type(Jws2020SignatureSuite.ID);
        builder.set(DataIntegrityVocab.PURPOSE).id(proofPurpose);
        builder.set(DataIntegrityVocab.CREATED).xsdDateTime(created != null ? created : Instant.now());

        return builder.build();
    }

    public static final class Builder {
        private Instant created;
        private URI proofPurpose;
        private VerificationMethod method;
        private URI id;
        private ObjectMapper mapper;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder created(Instant created) {
            this.created = created;
            return this;
        }

        public Builder verificationMethod(VerificationMethod verificationMethod) {
            this.method = verificationMethod;
            return this;
        }

        public Builder proofPurpose(URI proofPurpose) {
            this.proofPurpose = proofPurpose;
            return this;
        }

        public Builder method(VerificationMethod method) {
            this.method = method;
            return this;
        }

        public Builder id(URI id) {
            this.id = id;
            return this;
        }

        public Builder mapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Jws2020ProofDraft build() {
            Objects.requireNonNull(mapper, "mapper is required");
            var draft = new Jws2020ProofDraft(new Jws2020CryptoSuite(), method, created, proofPurpose);
            draft.id = this.id;
            draft.mapper = mapper;
            return draft;
        }
    }
}
