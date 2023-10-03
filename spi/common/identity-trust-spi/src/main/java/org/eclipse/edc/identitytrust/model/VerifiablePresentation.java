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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a VerifiablePresentation object as specified by the
 * <a href="https://w3c.github.io/vc-data-model/#presentations-0">W3 specification</a>
 */
public class VerifiablePresentation {
    public static final String DEFAULT_TYPE = "VerifiablePresentation";
    public static final String VERIFIABLE_PRESENTATION_ID_PROPERTY = "id";
    public static final String VERIFIABLE_PRESENTATION_TYPE_PROPERTY = "type";
    public static final String VERIFIABLE_PRESENTATION_HOLDER_PROPERTY = "holder";
    public static final String VERIFIABLE_PRESENTATION_VC_PROPERTY = "verifiableCredential";
    public static final String VERIFIABLE_PRESENTATION_PROOF_PROPERTY = "proof";
    private final String rawVp;
    private final CredentialFormat format;
    private List<VerifiableCredential> credentials = new ArrayList<>();
    private String id;
    private List<String> types = new ArrayList<>();
    private String holder; //must be a URI

    private VerifiablePresentation(String rawVp, CredentialFormat format) {
        types.add(DEFAULT_TYPE);
        this.rawVp = rawVp;
        this.format = format;
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

    public CredentialFormat getFormat() {
        return format;
    }

    public String getRawVp() {
        return rawVp;
    }


    public static final class Builder {
        private final VerifiablePresentation instance;

        private Builder(String rawVp, CredentialFormat format) {
            this.instance = new VerifiablePresentation(rawVp, format);
        }

        public static Builder newInstance(String rawVp, CredentialFormat format) {
            return new Builder(rawVp, format);
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

        public VerifiablePresentation build() {
            if (instance.types == null || instance.types.isEmpty()) {
                throw new IllegalArgumentException("VerifiablePresentation must have at least one type.");
            }
            // these next two aren't mandated by the spec, but there is no point in having a VP without credentials or a proof.
            if (instance.credentials == null || instance.credentials.isEmpty()) {
                throw new IllegalArgumentException("VerifiablePresentation must have at least one credential.");
            }
            return instance;
        }
    }
}

