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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.SCHEMA_ORG_NAMESPACE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX;

/**
 * Represents a VerifiableCredential as per the <a href="https://w3c.github.io/vc-data-model/">VerifiableCredential Data Model 2.0</a>.
 * Note that the proof is not maintained in this data structure.
 */
public class VerifiableCredential {
    public static final String VERIFIABLE_CREDENTIAL_ISSUER_PROPERTY = VC_PREFIX + "issuer";
    public static final String VERIFIABLE_CREDENTIAL_ISSUANCEDATE_PROPERTY = VC_PREFIX + "issuanceDate";
    public static final String VERIFIABLE_CREDENTIAL_EXPIRATIONDATE_PROPERTY = VC_PREFIX + "expirationDate";
    public static final String VERIFIABLE_CREDENTIAL_VALIDFROM_PROPERTY = VC_PREFIX + "validFrom";
    public static final String VERIFIABLE_CREDENTIAL_VALIDUNTIL_PROPERTY = VC_PREFIX + "validUntil";
    public static final String VERIFIABLE_CREDENTIAL_STATUS_PROPERTY = VC_PREFIX + "credentialStatus";
    public static final String VERIFIABLE_CREDENTIAL_SUBJECT_PROPERTY = VC_PREFIX + "credentialSubject";
    public static final String VERIFIABLE_CREDENTIAL_SCHEMA_PROPERTY = VC_PREFIX + "credentialSchema";
    public static final String VERIFIABLE_CREDENTIAL_NAME_PROPERTY = SCHEMA_ORG_NAMESPACE + "name";
    public static final String VERIFIABLE_CREDENTIAL_DESCRIPTION_PROPERTY = SCHEMA_ORG_NAMESPACE + "description";
    public static final String VERIFIABLE_CREDENTIAL_PROOF_PROPERTY = "https://w3id.org/security#proof";

    protected List<CredentialSubject> credentialSubject = new ArrayList<>();
    protected String id; // must be URI, but URI is less efficient at runtime

    protected List<String> type = new ArrayList<>();
    protected Issuer issuer; // can be URI or an object containing an ID
    protected Instant issuanceDate; // // VCDM 2.0 calls this "validFrom"
    protected Instant expirationDate; // VCDM 2.0 calls this "validUntil"
    protected List<CredentialStatus> credentialStatus = new ArrayList<>();
    protected String description;
    protected String name;
    protected DataModelVersion dataModelVersion = DataModelVersion.V_1_1;
    protected List<CredentialSchema> credentialSchema = new ArrayList<>();

    protected VerifiableCredential() {
    }

    public List<CredentialSchema> getCredentialSchema() {
        return credentialSchema;
    }

    public List<CredentialSubject> getCredentialSubject() {
        return credentialSubject;
    }

    public String getId() {
        return id;
    }

    public List<String> getType() {
        return type;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    @JsonAlias({"issued", "validFrom"}) // some credentials like StatusList2021 don't adhere to the spec
    @NotNull
    public Instant getIssuanceDate() {
        return issuanceDate;
    }

    @JsonAlias({"validUntil"})
    public Instant getExpirationDate() {
        return expirationDate;
    }

    // for VCDM 2.0
    @JsonIgnore
    public Instant getValidFrom() {
        return issuanceDate;
    }

    // for VCDM 2.0
    @JsonIgnore
    public Instant getValidUntil() {
        return expirationDate;
    }

    public List<CredentialStatus> getCredentialStatus() {
        return credentialStatus;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public DataModelVersion getDataModelVersion() {
        return dataModelVersion;
    }

    public static class Builder<T extends VerifiableCredential, B extends Builder<T, B>> {
        protected final T instance;

        protected Builder(T credential) {
            instance = credential;
        }

        public static Builder newInstance() {
            return new Builder(new VerifiableCredential());
        }

        public B credentialSubjects(List<CredentialSubject> credentialSubject) {
            this.instance.credentialSubject = credentialSubject;
            return self();
        }

        public B credentialSubject(CredentialSubject subject) {
            this.instance.credentialSubject.add(subject);
            return self();
        }

        public B id(String id) {
            this.instance.id = id;
            return self();
        }

        public B types(List<String> type) {
            this.instance.type = type;
            return self();
        }

        public B type(String type) {
            this.instance.type.add(type);
            return self();
        }

        public B description(String desc) {
            this.instance.description = desc;
            return self();
        }

        /**
         * Issuers can be URIs or objects containing an ID
         */
        public B issuer(Issuer issuer) {
            this.instance.issuer = issuer;
            return self();
        }

        public B issuanceDate(Instant issuanceDate) {
            this.instance.issuanceDate = issuanceDate;
            return self();
        }

        public B expirationDate(Instant expirationDate) {
            this.instance.expirationDate = expirationDate;
            return self();
        }

        // yes, the plural of "status" is "status"
        public B credentialStatus(List<CredentialStatus> credentialStatus) {
            this.instance.credentialStatus = credentialStatus;
            return self();
        }

        public B credentialStatus(CredentialStatus credentialStatus) {
            this.instance.credentialStatus.add(credentialStatus);
            return self();
        }

        public B name(String name) {
            this.instance.name = name;
            return self();
        }

        public B dataModelVersion(DataModelVersion dataModelVersion) {
            this.instance.dataModelVersion = dataModelVersion;
            return self();
        }

        public B credentialSchemas(List<CredentialSchema> credentialSchemas) {
            this.instance.credentialSchema = credentialSchemas;
            return self();
        }

        public B credentialSchema(CredentialSchema credentialSchema) {
            this.instance.credentialSchema.add(credentialSchema);
            return self();
        }

        public T build() {
            if (instance.type.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredentials MUST have at least one 'type' value.");
            }
            if (instance.credentialSubject == null || instance.credentialSubject.isEmpty()) {
                throw new IllegalArgumentException("VerifiableCredential must have a non-null, non-empty 'credentialSubject' property.");
            }
            Objects.requireNonNull(instance.issuer, "VerifiableCredential must have an 'issuer' property.");
            Objects.requireNonNull(instance.issuanceDate, "Credential must contain `issuanceDate`/`validFrom` property.");

            return instance;
        }

        protected B self() {
            return (B) this;
        }
    }
}
