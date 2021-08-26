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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.nimbusds.jose.jwk.ECKey;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = DidDocument.Builder.class)
public class DidDocument {
    String id;
    String context = "https://w3id.org/did-resolution/v1";
    List<Service> services = new ArrayList<>();
    List<VerificationMethod> verificationMethod = new ArrayList<>();
    List<String> authentication = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContext() {
        return context;
    }

    public List<Service> getServices() {
        return services;
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

        public Builder context(String context) {
            this.document.context = context;
            return this;
        }

        public Builder services(List<Service> services) {
            this.document.services.addAll(services);
            return this;
        }

        public Builder service(Service service) {
            this.document.services.add(service);
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


