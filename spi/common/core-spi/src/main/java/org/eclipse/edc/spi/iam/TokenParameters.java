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
 * Parameter Object for {@link IdentityService#obtainClientCredentials(TokenParameters)}.
 */
public class TokenParameters {
    private final Map<String, Object> additional = new HashMap<>();
    private String scope;
    private String audience;

    private TokenParameters() {
    }

    public String getScope() {
        return scope;
    }

    public String getAudience() {
        return audience;
    }

    public Map<String, Object> getAdditional() {
        return additional;
    }

    public static class Builder {
        private final TokenParameters result;

        private Builder() {
            result = new TokenParameters();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder scope(String scope) {
            result.scope = scope;
            return this;
        }

        public Builder audience(String audience) {
            result.audience = audience;
            return this;
        }

        public Builder additional(Map<String, Object> additional) {
            result.additional.putAll(additional);
            return this;
        }

        public Builder additional(String key, Object value) {
            result.additional.put(key, value);
            return this;
        }

        public TokenParameters build() {
            return result;
        }
    }
}
