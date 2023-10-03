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

package org.eclipse.edc.identitytrust.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Represents a VerifiableCredential as per the <a href="https://www.w3.org/TR/vc-data-model/">VerifiableCredential Data Model 1.1</a>
 */
public class VerifiableCredential {
    public static final String DEFAULT_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    public static final String DEFAULT_TYPE = "VerifiableCredential";

    private List<String> contexts = new ArrayList<>();
    private List<CredentialSubject> credentialSubject = new ArrayList<>();
    private List<Proof> proofs = new ArrayList<>();
    private URI id;
    private List<String> types = new ArrayList<>();
    private Object issuer; // can be URI or an object containing an ID
    private Date issuanceDate;
    private Date expirationDate;
    private CredentialStatus credentialStatus;

    private VerifiableCredential() {
    }

    public List<String> getContexts() {
        return contexts;
    }

    public List<CredentialSubject> getCredentialSubject() {
        return credentialSubject;
    }

    public List<Proof> getProofs() {
        return proofs;
    }

    public URI getId() {
        return id;
    }

    public List<String> getTypes() {
        return types;
    }

    public Object getIssuer() {
        return issuer;
    }

    public Date getIssuanceDate() {
        return issuanceDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public CredentialStatus getCredentialStatus() {
        return credentialStatus;
    }

    public static final class Builder {
        private final VerifiableCredential instance;

        private Builder() {
            instance = new VerifiableCredential();
            instance.contexts.add(DEFAULT_CONTEXT);
            instance.types.add(DEFAULT_TYPE);
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contexts(List<String> contexts) {
            this.instance.contexts = contexts;
            return this;
        }

        public Builder context(String context) {
            this.instance.contexts.add(context);
            return this;
        }

        public Builder credentialSubject(List<CredentialSubject> credentialSubject) {
            this.instance.credentialSubject = credentialSubject;
            return this;
        }

        public Builder credentialSubject(CredentialSubject subject) {
            this.instance.credentialSubject.add(subject);
            return this;
        }

        public Builder proofs(List<Proof> proofs) {
            this.instance.proofs = proofs;
            return this;
        }

        public Builder proof(Proof proof) {
            this.instance.proofs.add(proof);
            return this;
        }

        public Builder id(URI id) {
            this.instance.id = id;
            return this;
        }

        public Builder types(List<String> type) {
            this.instance.types = type;
            return this;
        }

        public Builder type(String type) {
            this.instance.types.add(type);
            return this;
        }

        /**
         * Issuers can be URIs or objects containing an ID
         */
        public Builder issuer(Object issuer) {
            this.instance.issuer = issuer;
            return this;
        }

        public Builder issuanceDate(Date issuanceDate) {
            this.instance.issuanceDate = issuanceDate;
            return this;
        }

        public Builder expirationDate(Date expirationDate) {
            this.instance.expirationDate = expirationDate;
            return this;
        }

        public Builder credentialStatus(CredentialStatus credentialStatus) {
            this.instance.credentialStatus = credentialStatus;
            return this;
        }

        public VerifiableCredential build() {
            if (instance.contexts.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredential must contain at least one context.");
            }
            if (instance.types.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredentials MUST have at least one 'type' value.");
            }
            if (instance.credentialSubject == null || instance.credentialSubject.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredential must have a non-null, non-empty 'credentialSubject' property.");
            }
            Objects.requireNonNull(instance.issuer, "VerifiableCredential must have an 'issuer' property.");
            Objects.requireNonNull(instance.issuanceDate, "Credential must contain `issuanceDate` property.");
            if (instance.proofs.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredential must contain at least one 'proof'.");
            }

            return instance;
        }
    }
}
