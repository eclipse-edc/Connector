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


public class TokenRenewalResponsePayloadToken {

    @JsonProperty("lease_duration")
    private long timeToLive;

    public long getTimeToLive() {
        return timeToLive;
    }

    private TokenRenewalResponsePayloadToken() {}

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenRenewalResponsePayloadToken tokenRenewalResponsePayloadToken;

        private Builder() {
            tokenRenewalResponsePayloadToken = new TokenRenewalResponsePayloadToken();
        }

        public static TokenRenewalResponsePayloadToken.Builder newInstance() {
            return new TokenRenewalResponsePayloadToken.Builder();
        }

        public TokenRenewalResponsePayloadToken.Builder timeToLive(long timeToLive) {
            tokenRenewalResponsePayloadToken.timeToLive = timeToLive;
            return this;
        }

        public TokenRenewalResponsePayloadToken build() {
            return tokenRenewalResponsePayloadToken;
        }
    }
}
