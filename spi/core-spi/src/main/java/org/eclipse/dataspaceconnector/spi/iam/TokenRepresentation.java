/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of an obtained or incoming token.
 */
public class TokenRepresentation {
    private String token;
    private long expiresIn;
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
     * Returns the token expiration.
     */
    public long expiresIn() {
        return expiresIn;
    }

    public Map<String, Object> getAdditional() {
        return additional;
    }

    public static class Builder {
        private final TokenRepresentation result;

        private Builder() {
            result = new TokenRepresentation();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder token(String token) {
            result.token = token;
            return this;
        }

        public Builder expiresIn(long expiresIn) {
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
