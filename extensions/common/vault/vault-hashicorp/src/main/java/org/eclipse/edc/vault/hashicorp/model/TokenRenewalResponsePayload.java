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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

/**
 * Token renewal response returned by Vault. Contains information about the renewal operation and token data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenRenewalResponsePayload {
    @JsonProperty("warnings")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> warnings;

    @JsonProperty("auth")
    private TokenRenewalResponsePayloadToken token;

    private TokenRenewalResponsePayload() {}

    public List<String> getWarnings() {
        return warnings;
    }

    public TokenRenewalResponsePayloadToken getToken() {
        return token;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenRenewalResponsePayload tokenRenewalResponsePayload;

        private Builder() {
            tokenRenewalResponsePayload = new TokenRenewalResponsePayload();
        }

        public static TokenRenewalResponsePayload.Builder newInstance() {
            return new TokenRenewalResponsePayload.Builder();
        }

        public TokenRenewalResponsePayload.Builder token(TokenRenewalResponsePayloadToken token) {
            tokenRenewalResponsePayload.token = token;
            return this;
        }

        public TokenRenewalResponsePayload build() {
            return tokenRenewalResponsePayload;
        }
    }
}


