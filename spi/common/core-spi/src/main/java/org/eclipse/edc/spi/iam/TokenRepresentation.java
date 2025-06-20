/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Simplified token representation
 *
 */

package org.eclipse.edc.spi.iam;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of an obtained or incoming token.
 */
public class TokenRepresentation {
    private String token;
    private Long expiresIn;
    private Map<String, Object> additional;

    private TokenRepresentation() {
        additional = new HashMap<>();
    }

    /**
     * Returns the bearer token if existent otherwise null.
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the lifetime of the token in seconds
     */
    public Long getExpiresIn() {
        return expiresIn;
    }

    public Map<String, Object> getAdditional() {
        return additional;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final TokenRepresentation result;

        private Builder(TokenRepresentation tokenRepresentation) {
            result = tokenRepresentation;
        }

        public static Builder newInstance() {
            return new Builder(new TokenRepresentation());
        }

        public Builder token(String token) {
            result.token = token;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            result.expiresIn = expiresIn;
            return this;
        }

        public Builder additional(Map<String, Object> additional) {
            result.additional = additional;
            return this;
        }

        public TokenRepresentation build() {
            return result;
        }
    }
}
