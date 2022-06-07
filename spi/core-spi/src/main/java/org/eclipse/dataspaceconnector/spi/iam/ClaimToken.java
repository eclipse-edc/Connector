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
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import java.util.HashMap;
import java.util.Map;

/**
 * Models a token containing claims such as a JWT.
 * Currently only a String representation of claims values is supported.
 */
public class ClaimToken {
    private final Map<String, Object> claims = new HashMap<>();

    private ClaimToken() {
    }

    /**
     * Returns the claims.
     */
    public Map<String, Object> getClaims() {
        return claims;
    }

    public static class Builder {
        private final ClaimToken token;

        private Builder() {
            token = new ClaimToken();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder claim(String key, Object value) {
            token.claims.put(key, value);
            return this;
        }

        public Builder claims(Map<String, Object> map) {
            token.claims.putAll(map);
            return this;
        }

        public ClaimToken build() {
            return token;
        }
    }
}
