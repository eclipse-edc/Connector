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

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenLookUpResponsePayloadToken {

    private static final String ROOT_POLICY = "root";

    @JsonProperty("explicit_max_ttl")
    private long explicitMaxTimeToLive;

    @JsonProperty("renewable")
    private boolean isRenewable;

    @JsonProperty("period")
    private Long period;

    @JsonProperty("policies")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> policies;

    public long getExplicitMaxTimeToLive() {
        return explicitMaxTimeToLive;
    }

    public boolean hasExplicitMaxTimeToLive() {
        return explicitMaxTimeToLive > 0;
    }

    public boolean isRenewable() {
        return isRenewable;
    }

    public Long getPeriod() {
        return period;
    }

    public boolean isPeriodicToken() {
        return period != null;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public boolean isRootToken() {
        return policies.contains(ROOT_POLICY);
    }


    private TokenLookUpResponsePayloadToken() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenLookUpResponsePayloadToken tokenLookUpResponsePayloadToken;

        private Builder() {
            tokenLookUpResponsePayloadToken = new TokenLookUpResponsePayloadToken();
        }

        public static TokenLookUpResponsePayloadToken.Builder newInstance() {
            return new TokenLookUpResponsePayloadToken.Builder();
        }

        public TokenLookUpResponsePayloadToken.Builder explicitMaxTimeToLive(long explicitMaxTtl) {
            tokenLookUpResponsePayloadToken.explicitMaxTimeToLive = explicitMaxTtl;
            return this;
        }

        public TokenLookUpResponsePayloadToken.Builder isRenewable(boolean isRenewable) {
            tokenLookUpResponsePayloadToken.isRenewable = isRenewable;
            return this;
        }

        public TokenLookUpResponsePayloadToken.Builder period(Long period) {
            tokenLookUpResponsePayloadToken.period = period;
            return this;
        }

        public TokenLookUpResponsePayloadToken.Builder policies(List<String> policies) {
            tokenLookUpResponsePayloadToken.policies = policies;
            return this;
        }

        public TokenLookUpResponsePayloadToken build() {
            return tokenLookUpResponsePayloadToken;
        }
    }
}
