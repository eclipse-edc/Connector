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

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.identitytrust.VcConstants.VC_PREFIX;

/**
 * Credential subject as defined in <a href="https://www.w3.org/TR/vc-data-model/#credential-subject">W3C specification</a>.
 */
public class CredentialSubject {
    public static final String CREDENTIAL_SUBJECT_ID_PROPERTY = VC_PREFIX + "id";
    private Map<String, Object> claims = new HashMap<>();
    private String id;

    public Map<String, Object> getClaims() {
        return claims;
    }

    public String getId() {
        return id;
    }

    public static final class Builder {
        private final CredentialSubject instance;

        private Builder() {
            instance = new CredentialSubject();
        }

        public static Builder newInstance() {
            return new Builder();
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
