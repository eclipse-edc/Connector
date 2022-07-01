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

package org.eclipse.dataspaceconnector.spi.iam;

import java.util.Objects;

/**
 * Parameter Object for {@link IdentityService#obtainClientCredentials(TokenParameters)}.
 */
public class TokenParameters {
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


        public TokenParameters build() {
            Objects.requireNonNull(result.audience, "audience");
            return result;
        }
    }
}
