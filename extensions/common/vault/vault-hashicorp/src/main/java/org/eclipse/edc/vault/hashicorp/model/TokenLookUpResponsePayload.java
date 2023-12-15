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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenLookUpResponsePayload {
    @JsonProperty("data")
    private TokenLookUpResponsePayloadToken token;

    public TokenLookUpResponsePayloadToken getToken() {
        return token;
    }

    private TokenLookUpResponsePayload() {}

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenLookUpResponsePayload tokenLookUpResponsePayload;

        private Builder() {
            tokenLookUpResponsePayload = new TokenLookUpResponsePayload();
        }

        public static TokenLookUpResponsePayload.Builder newInstance() {
            return new TokenLookUpResponsePayload.Builder();
        }

        public TokenLookUpResponsePayload.Builder token(TokenLookUpResponsePayloadToken token) {
            tokenLookUpResponsePayload.token = token;
            return this;
        }

        public TokenLookUpResponsePayload build() {
            return tokenLookUpResponsePayload;
        }
    }
}

