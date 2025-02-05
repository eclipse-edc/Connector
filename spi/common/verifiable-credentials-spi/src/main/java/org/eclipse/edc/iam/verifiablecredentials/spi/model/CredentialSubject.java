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
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX;

/**
 * Credential subject as defined in <a href="https://www.w3.org/TR/vc-data-model/#credential-subject">W3C specification</a>.
 */
public class CredentialSubject {
    public static final String CREDENTIAL_SUBJECT_ID_PROPERTY = VC_PREFIX + "id";
    private Map<String, Object> claims = new HashMap<>();
    private String id;

    @JsonAnyGetter
    public Map<String, Object> getClaims() {
        return claims;
    }

    @JsonAnySetter
    public void setClaim(String name, Object value) {
        claims.put(name, value);
    }


    /**
     * Returns a claim if presents in the claims map. This method
     * will try first the combination namespace + property and if
     * not found will fall back to just property when fetching the claim
     * from the underling map
     *
     * @param namespace The namespace of the property
     * @param property  The name of the property
     * @return The claim if present, null otherwise
     */
    public Object getClaim(String namespace, String property) {
        return ofNullable(claims.get(namespace + property))
                .or(() -> ofNullable(claims.get(property)))
                .orElse(null);
    }

    public String getId() {
        return id;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private final CredentialSubject instance;

        private Builder(CredentialSubject instance) {
            this.instance = instance;
        }

        public static Builder newInstance() {
            return new Builder(new CredentialSubject());
        }

        public Builder claims(Map<String, Object> claims) {
            this.instance.claims = claims;
            return this;
        }

        public Builder claim(String key, Object value) {
            this.instance.claims.put(key, value);
            return this;
        }

        public Builder id(String id) {
            this.instance.id = id;
            return this;
        }

        public CredentialSubject build() {
            if (instance.claims == null || instance.claims.isEmpty()) {
                throw new IllegalArgumentException("CredentialSubject must contain claims");
            }
            return instance;
        }
    }
}
