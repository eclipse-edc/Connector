/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Represents a Vault token renewal request.
 */
public class TokenRenewRequest {
    private static final String INCREMENT_SECONDS_FORMAT = "%ds";

    private String increment;

    private TokenRenewRequest() {}

    public String getIncrement() {
        return increment;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenRenewRequest tokenRenewalRequest;

        private Builder() {
            tokenRenewalRequest = new TokenRenewRequest();
        }

        @JsonCreator
        public static TokenRenewRequest.Builder newInstance() {
            return new TokenRenewRequest.Builder();
        }

        @JsonProperty("increment")
        public TokenRenewRequest.Builder increment(long increment) {
            tokenRenewalRequest.increment = INCREMENT_SECONDS_FORMAT.formatted(increment);
            return this;
        }

        public TokenRenewRequest build() {
            return tokenRenewalRequest;
        }
    }
}

