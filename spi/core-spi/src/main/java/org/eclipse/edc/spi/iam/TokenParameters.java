/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.edc.spi.iam;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameter Object for {@link IdentityService#obtainClientCredentials(String, TokenParameters)}.
 */
public class TokenParameters {
    private final Map<String, Object> claims = new HashMap<>();
    private Map<String, Object> headers = new HashMap<>();

    private TokenParameters() {
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public String getStringClaim(String key) {
        return (String) claims.get(key);
    }

    public String getStringHeader(String key) {
        return (String) headers.get(key);
    }

    public static class Builder {
        private final TokenParameters result;

        private Builder() {
            result = new TokenParameters();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder claims(Map<String, Object> additional) {
            result.claims.putAll(additional);
            return this;
        }

        public Builder claims(String key, Object value) {
            result.claims.put(key, value);
            return this;
        }

        public Builder header(String key, Object value) {
            this.result.headers.put(key, value);
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.result.headers = headers;
            return this;
        }

        public TokenParameters build() {
            return result;
        }
    }
}
