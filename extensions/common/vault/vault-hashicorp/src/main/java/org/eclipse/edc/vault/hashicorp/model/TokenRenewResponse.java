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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

/**
 * Token renewal response returned by Vault. Contains the actual token data and meta information.
 */
@JsonDeserialize(builder = TokenRenewResponse.Builder.class)
public class TokenRenewResponse {
    private Object data;

    private TokenRenewAuth auth;

    private boolean renewable;

    private List<String> warnings;

    private long leaseDuration;

    private String requestId;

    private String leaseId;

    private TokenRenewResponse() {
    }

    public Object getData() {
        return data;
    }

    public TokenRenewAuth getAuth() {
        return auth;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getLeaseId() {
        return leaseId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenRenewResponse tokenRenewResponse;

        private Builder() {
            tokenRenewResponse = new TokenRenewResponse();
        }

        @JsonCreator
        public static TokenRenewResponse.Builder newInstance() {
            return new TokenRenewResponse.Builder();
        }

        @JsonProperty("data")
        public TokenRenewResponse.Builder token(Object data) {
            tokenRenewResponse.data = data;
            return this;
        }

        @JsonProperty("auth")
        public TokenRenewResponse.Builder auth(TokenRenewAuth auth) {
            tokenRenewResponse.auth = auth;
            return this;
        }

        @JsonProperty("renewable")
        public TokenRenewResponse.Builder renewable(boolean renewable) {
            tokenRenewResponse.renewable = renewable;
            return this;
        }

        @JsonSetter(value = "warnings", nulls = Nulls.AS_EMPTY)
        public TokenRenewResponse.Builder warnings(List<String> warnings) {
            tokenRenewResponse.warnings = warnings;
            return this;
        }

        @JsonProperty("lease_duration")
        public TokenRenewResponse.Builder leaseDuration(long leaseDuration) {
            tokenRenewResponse.leaseDuration = leaseDuration;
            return this;
        }

        @JsonProperty("request_id")
        public TokenRenewResponse.Builder requestId(String requestId) {
            tokenRenewResponse.requestId = requestId;
            return this;
        }

        @JsonProperty("lease_id")
        public TokenRenewResponse.Builder leaseId(String leaseId) {
            tokenRenewResponse.leaseId = leaseId;
            return this;
        }

        public TokenRenewResponse build() {
            return tokenRenewResponse;
        }
    }
}