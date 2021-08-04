/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.iam;

import java.util.HashMap;
import java.util.Map;

/**
 * Models a token containing claims such as a JWT.
 *
 * Currently only a String representation of claims values is supported.
 */
public class ClaimToken {
    private Map<String, String> claims = new HashMap<>();

    /**
     * Returns the claims.
     */
    public Map<String, String> getClaims() {
        return claims;
    }

    private ClaimToken() {
    }

    public static class Builder {
        private ClaimToken token;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder claim(String key, String value) {
            token.claims.put(key, value);
            return this;
        }

        public Builder claims(Map<String, String> map) {
            token.claims.putAll(map);
            return this;
        }

        public ClaimToken build() {
            return token;
        }

        private Builder() {
            token = new ClaimToken();
        }
    }
}
