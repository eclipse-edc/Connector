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
 *       Mercedes-Benz Tech Innovation GmbH - Add token rotation mechanism
 *
 */

package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class TokenRenewalRequestPayload {
    private static final String INCREMENT_SECONDS_FORMAT = "%ds";

    @JsonProperty("increment")
    private String increment;

    private TokenRenewalRequestPayload() {}

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenRenewalRequestPayload tokenRenewalRequestPayload;

        private Builder() {
            tokenRenewalRequestPayload = new TokenRenewalRequestPayload();
        }

        public static TokenRenewalRequestPayload.Builder newInstance() {
            return new TokenRenewalRequestPayload.Builder();
        }

        public TokenRenewalRequestPayload.Builder increment(int increment) {
            tokenRenewalRequestPayload.increment = INCREMENT_SECONDS_FORMAT.formatted(increment);
            return this;
        }

        public TokenRenewalRequestPayload build() {
            return tokenRenewalRequestPayload;
        }
    }
}

