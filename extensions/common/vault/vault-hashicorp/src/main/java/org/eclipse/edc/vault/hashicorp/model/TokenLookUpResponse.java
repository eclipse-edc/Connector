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
 * Vault Token lookup response returned by the Vault. Contains the actual token data and meta information.
 */
@JsonDeserialize(builder = TokenLookUpResponse.Builder.class)
public class TokenLookUpResponse {

    private TokenLookUpData data;

    private boolean renewable;

    private List<String> warnings;
    private Object wrapInfo;

    private int leaseDuration;
    private String requestId;

    private String leaseId;

    private TokenLookUpResponse() {}

    public TokenLookUpData getData() {
        return data;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public List<String> getWarnings() {
        return warnings;
    }
    public Object getWrapInfo() {
        return wrapInfo;
    }

    public int getLeaseDuration() {
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
        private final TokenLookUpResponse tokenLookUpResponse;

        private Builder() {
            tokenLookUpResponse = new TokenLookUpResponse();
        }

        @JsonCreator
        public static TokenLookUpResponse.Builder newInstance() {
            return new TokenLookUpResponse.Builder();
        }

        @JsonProperty("data")
        public TokenLookUpResponse.Builder data(TokenLookUpData data) {
            tokenLookUpResponse.data = data;
            return this;
        }

        @JsonProperty("renewable")
        public TokenLookUpResponse.Builder renewable(boolean renewable) {
            tokenLookUpResponse.renewable = renewable;
            return this;
        }

        @JsonSetter(value = "warnings", nulls = Nulls.AS_EMPTY)
        public TokenLookUpResponse.Builder warnings(List<String> warnings) {
            tokenLookUpResponse.warnings = warnings;
            return this;
        }

        @JsonProperty("wrap_info")
        public Object wrapInfo(Object wrapInfo) {
            tokenLookUpResponse.wrapInfo = wrapInfo;
            return this;
        }

        @JsonProperty("lease_duration")
        public TokenLookUpResponse.Builder leaseDuration(int leaseDuration) {
            tokenLookUpResponse.leaseDuration = leaseDuration;
            return this;
        }

        @JsonProperty("request_id")
        public TokenLookUpResponse.Builder requestId(String requestId) {
            tokenLookUpResponse.requestId = requestId;
            return this;
        }

        @JsonProperty("lease_id")
        public TokenLookUpResponse.Builder leaseId(String leaseId) {
            tokenLookUpResponse.leaseId = leaseId;
            return this;
        }

        public TokenLookUpResponse build() {
            return tokenLookUpResponse;
        }
    }
}