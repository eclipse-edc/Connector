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

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX;

/**
 * Represents a VerifiablePresentation object as specified by the
 * <a href="https://w3c.github.io/vc-data-model/#presentations-0">W3 specification</a>
 */
public class VerifiablePresentation {
    public static final String VERIFIABLE_PRESENTATION_HOLDER_PROPERTY = VC_PREFIX + "holder";
    public static final String VERIFIABLE_PRESENTATION_VC_PROPERTY = VC_PREFIX + "verifiableCredential";
    public static final String VERIFIABLE_PRESENTATION_PROOF_PROPERTY = "https://w3id.org/security#proof";
    private List<VerifiableCredential> credentials = new ArrayList<>();
    private String id;
    private List<String> types = new ArrayList<>();
    private String holder; //must be a URI
    protected DataModelVersion dataModelVersion = DataModelVersion.V_1_1;

    private VerifiablePresentation() {
    }

    public List<VerifiableCredential> getCredentials() {
        return credentials;
    }

    public String getId() {
        return id;
    }

    public List<String> getTypes() {
        return types;
    }

    public String getHolder() {
        return holder;
    }

    public DataModelVersion getDataModelVersion() {
        return dataModelVersion;
    }

    public static final class Builder {
        private final VerifiablePresentation instance;

        private Builder() {
            this.instance = new VerifiablePresentation();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentials(List<VerifiableCredential> credentials) {
            this.instance.credentials = credentials;
            return this;
        }

        public Builder credential(VerifiableCredential credential) {
            this.instance.credentials.add(credential);
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

        public Builder holder(String holder) {
            this.instance.holder = holder;
            return this;
        }

        public Builder dataModelVersion(DataModelVersion dataModelVersion) {
            this.instance.dataModelVersion = dataModelVersion;
            return this;
        }

        public VerifiablePresentation build() {
            if (instance.types == null || instance.types.isEmpty()) {
                throw new IllegalArgumentException("VerifiablePresentation must have at least one type.");
            }
            return instance;
        }
    }
}

