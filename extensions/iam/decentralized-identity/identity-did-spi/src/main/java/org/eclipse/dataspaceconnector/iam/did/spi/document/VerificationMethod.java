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

package org.eclipse.dataspaceconnector.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = VerificationMethod.Builder.class)
public class VerificationMethod {
    private String id;
    private String controller;
    private String type;
    private JwkPublicKey publicKeyJwk;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("controller")
    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("publicKeyJwk")
    public JwkPublicKey getPublicKeyJwk() {
        return publicKeyJwk;
    }

    public void setPublicKeyJwk(JwkPublicKey publicKeyJwk) {
        this.publicKeyJwk = publicKeyJwk;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String id;
        private String controller;
        private String type;
        private JwkPublicKey publicKeyJwk;

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

        public Builder controller(String controller) {
            this.controller = controller;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder publicKeyJwk(JwkPublicKey publicKeyJwk) {
            this.publicKeyJwk = publicKeyJwk;
            return this;
        }

        public VerificationMethod build() {
            VerificationMethod verificationMethod = new VerificationMethod();
            verificationMethod.setId(id);
            verificationMethod.setController(controller);
            verificationMethod.setType(type);
            verificationMethod.setPublicKeyJwk(publicKeyJwk);
            return verificationMethod;
        }
    }
}
