/*
 *  Copyright (c) 2020, 2020-2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

/**
 * Wrapper object for the "publicKey" part of a DID create-request, that contains additional information such as the type of public key, its purposes and
 * an ID.
 */
@JsonDeserialize(builder = PublicKeyDescriptor.Builder.class)
public class PublicKeyDescriptor {
    private String id;
    private String type;
    private Map<String, String> publicKeyJwk;
    private String[] purposes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getPublicKeyJwk() {
        return publicKeyJwk;
    }

    public void setPublicKeyJwk(Map<String, String> publicKeyJwk) {
        this.publicKeyJwk = publicKeyJwk;
    }

    public String[] getPurposes() {
        return purposes;
    }

    public void setPurposes(String[] purposes) {
        this.purposes = purposes;
    }


    @JsonPOJOBuilder
    public static final class Builder {
        private String id;
        private String type;
        private Map<String, String> publicKeyJwk;
        private String[] purposes = {"authentication"};

        private Builder() {
        }

        @JsonCreator
        public static Builder create() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder publicKeyJwk(Map<String, String> publicKeyJwk) {
            this.publicKeyJwk = publicKeyJwk;
            return this;
        }

        public Builder purposes(String... purposes) {
            this.purposes = purposes;
            return this;
        }

        public PublicKeyDescriptor build() {
            PublicKeyDescriptor publicKeyDescriptor = new PublicKeyDescriptor();
            publicKeyDescriptor.id = id;
            publicKeyDescriptor.publicKeyJwk = publicKeyJwk;
            publicKeyDescriptor.purposes = purposes;
            publicKeyDescriptor.type = type;
            return publicKeyDescriptor;
        }
    }
}
