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

package org.eclipse.edc.iam.did.spi.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.edc.spi.EdcException;

import java.text.ParseException;
import java.util.Map;

@JsonDeserialize(builder = VerificationMethod.Builder.class)
public class VerificationMethod {
    private String id;
    private String type;
    private String controller;
    private String publicKeyMultibase;
    private Map<String, Object> publicKeyJwk;

    private VerificationMethod() {

    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getController() {
        return controller;
    }

    public String getPublicKeyMultibase() {
        return publicKeyMultibase;
    }

    public Map<String, Object> getPublicKeyJwk() {
        return publicKeyJwk;
    }

    /**
     * Serializes the public key of the VerificationMethod.
     * If publicKeyJwt is not null, it serializes it to a JSON string and returns its bytes.
     * If publicKeyMultibase is not null, it simply returns its bytes.
     * If both are null, an {@link IllegalStateException} is thrown.
     *
     * @return the serialized public key as a byte array
     * @throws EdcException if an error occurs during serialization
     */
    public byte[] serializePublicKey() {
        if (publicKeyJwk != null) {
            try {
                var jwk = JWK.parse(publicKeyJwk);
                return jwk.toJSONString().getBytes();
            } catch (ParseException e) {
                throw new EdcException(e);
            }
        } else if (publicKeyMultibase != null) {
            return publicKeyMultibase.getBytes();
        }

        throw new IllegalStateException("Either publicKeyJwk or publicKeyMultibase must be present, not both, not neither.");
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final VerificationMethod method;

        private Builder() {
            method = new VerificationMethod();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.method.id = id;
            return this;
        }

        public Builder controller(String controller) {
            this.method.controller = controller;
            return this;
        }

        public Builder type(String type) {
            this.method.type = type;
            return this;
        }

        public Builder publicKeyJwk(Map<String, Object> jwk) {
            this.method.publicKeyJwk = jwk;
            return this;
        }

        public Builder publicKeyMultibase(String multibase) {
            this.method.publicKeyMultibase = multibase;
            return this;
        }

        public VerificationMethod build() {
            if (method.publicKeyMultibase != null && method.publicKeyJwk != null) {
                throw new IllegalArgumentException("Invalid public key material. Only one of publicKeyMultibase or publicKeyJwk should be provided.");
            }

            if (method.publicKeyMultibase == null && method.publicKeyJwk == null) {
                throw new IllegalArgumentException("One of publicKeyMultibase and publicKeyJwk must be provided.");
            }
            return this.method;
        }

    }


}
