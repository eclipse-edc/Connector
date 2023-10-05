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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.identitytrust.VcConstants.SCHEMA_ORG_NAMESPACE;
import static org.eclipse.edc.identitytrust.VcConstants.VC_PREFIX;

/**
 * Represents a VerifiableCredential as per the <a href="https://w3c.github.io/vc-data-model/">VerifiableCredential Data Model 2.0</a>.
 * The RAW VC must always be preserved in THE EXACT FORMAT it was originally received, otherwise the proofs become invalid.
 */
public class VerifiableCredential {
    public static final String VERIFIABLE_CREDENTIAL_ISSUER_PROPERTY = VC_PREFIX + "issuer";
    public static final String VERIFIABLE_CREDENTIAL_ISSUANCEDATE_PROPERTY = VC_PREFIX + "issuanceDate";
    public static final String VERIFIABLE_CREDENTIAL_EXPIRATIONDATE_PROPERTY = VC_PREFIX + "expirationDate";
    public static final String VERIFIABLE_CREDENTIAL_VALIDFROM_PROPERTY = VC_PREFIX + "validFrom";
    public static final String VERIFIABLE_CREDENTIAL_VALIDUNTIL_PROPERTY = VC_PREFIX + "validUntil";
    public static final String VERIFIABLE_CREDENTIAL_STATUS_PROPERTY = VC_PREFIX + "credentialStatus";
    public static final String VERIFIABLE_CREDENTIAL_SUBJECT_PROPERTY = VC_PREFIX + "credentialSubject";
    public static final String VERIFIABLE_CREDENTIAL_NAME_PROPERTY = SCHEMA_ORG_NAMESPACE + "name";
    public static final String VERIFIABLE_CREDENTIAL_DESCRIPTION_PROPERTY = SCHEMA_ORG_NAMESPACE + "description";
    private final String rawVc;
    private final CredentialFormat format;
    private List<CredentialSubject> credentialSubject = new ArrayList<>();
    private String id; // must be URI, but URI is less efficient at runtime
    private List<String> types = new ArrayList<>();
    private Object issuer; // can be URI or an object containing an ID
    private Instant issuanceDate; // v2 of the spec renames this to "validFrom"
    private Instant expirationDate; // v2 of the spec renames this to "validUntil"
    private CredentialStatus credentialStatus;
    private String description;
    private String name;

    private VerifiableCredential(String rawVc, CredentialFormat format) {
        this.rawVc = rawVc;
        this.format = format;
    }

    public List<CredentialSubject> getCredentialSubject() {
        return credentialSubject;
    }

    public String getId() {
        return id;
    }

    public List<String> getTypes() {
        return types;
    }

    public Object getIssuer() {
        return issuer;
    }

    public Instant getIssuanceDate() {
        return issuanceDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public CredentialStatus getCredentialStatus() {
        return credentialStatus;
    }

    public String getRawVc() {
        return rawVc;
    }

    public CredentialFormat getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public static final class Builder {
        private final VerifiableCredential instance;

        private Builder(String rawVc, CredentialFormat format) {
            instance = new VerifiableCredential(rawVc, format);
        }

        public static Builder newInstance(String rawVc, CredentialFormat format) {
            return new Builder(rawVc, format);
        }

        public Builder credentialSubject(List<CredentialSubject> credentialSubject) {
            this.instance.credentialSubject = credentialSubject;
            return this;
        }

        public Builder credentialSubject(CredentialSubject subject) {
            this.instance.credentialSubject.add(subject);
            return this;
        }

        public Builder id(String id) {
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

        public Builder description(String desc) {
            this.instance.description = desc;
            return this;
        }

        /**
         * Issuers can be URIs or objects containing an ID
         */
        public Builder issuer(Object issuer) {
            this.instance.issuer = issuer;
            return this;
        }

        public Builder issuanceDate(Instant issuanceDate) {
            this.instance.issuanceDate = issuanceDate;
            return this;
        }

        public Builder expirationDate(Instant expirationDate) {
            this.instance.expirationDate = expirationDate;
            return this;
        }

        public Builder credentialStatus(CredentialStatus credentialStatus) {
            this.instance.credentialStatus = credentialStatus;
            return this;
        }

        public Builder name(String name) {
            this.instance.name = name;
            return this;
        }

        public VerifiableCredential build() {
            if (instance.types.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredentials MUST have at least one 'type' value.");
            }
            if (instance.credentialSubject == null || instance.credentialSubject.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredential must have a non-null, non-empty 'credentialSubject' property.");
            }
            Objects.requireNonNull(instance.issuer, "VerifiableCredential must have an 'issuer' property.");
            Objects.requireNonNull(instance.issuanceDate, "Credential must contain `issuanceDate` property.");

            return instance;
        }


    }
}
