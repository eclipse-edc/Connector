/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.nimbusds.jose.jwk.ECKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonDeserialize(builder = DidDocument.Builder.class)
public class    DidDocument {
    String id;
    @JsonProperty("@context")
    List<Object> context = Collections.singletonList("https://w3id.org/did-resolution/v1");
    List<Service> service = new ArrayList<>();
    List<VerificationMethod> verificationMethod = new ArrayList<>();
    List<String> authentication = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Object> getContext() {
        return context;
    }

    public List<Service> getService() {
        return service;
    }

    public List<VerificationMethod> getVerificationMethod() {
        return verificationMethod;
    }

    public List<String> getAuthentication() {
        return authentication;
    }

    @Override
    public String toString() {
        return getId();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private DidDocument document;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.document.id = id;
            return this;
        }

        @JsonProperty("@context")
        public Builder context(List<Object> context) {
            this.document.context = context;
            return this;
        }

        public Builder service(List<Service> services) {
            this.document.service.addAll(services);
            return this;
        }


        public Builder verificationMethod(List<VerificationMethod> verificationMethod) {
            this.document.verificationMethod = verificationMethod;
            return this;
        }

        public Builder verificationMethod(String id, String type, ECKey publicKey) {
            document.verificationMethod.add(VerificationMethod.Builder.create()
                    .id(id)
                    .type(type)
                    .publicKeyJwk(new PublicKeyJwk(publicKey.getCurve().getName(), publicKey.getKeyType().getValue(), publicKey.getX().toString(), publicKey.getY().toString()))
                    .build());
            return this;
        }

        public Builder authentication(List<String> authentication) {
            this.document.authentication = authentication;
            return this;
        }

        private Builder() {
            document = new DidDocument();
        }

        public DidDocument build() {
            return document;
        }
    }
}


