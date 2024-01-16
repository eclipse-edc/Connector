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
import java.util.Map;

/**
 * Token data returned by the Vault renewal operation.
 */
@JsonDeserialize(builder = TokenRenewAuth.Builder.class)
public class TokenRenewAuth {

    private List<String> tokenPolicies;

    private String clientToken;

    private Map<String, String> metadata;

    private boolean renewable;

    private String accessor;

    private List<String> policies;

    private List<String> identityPolicies;

    private long leaseDuration;

    private Object mfaRequirement;

    private boolean orphan;

    private String entityId;

    private TokenRenewAuth() {
    }

    public List<String> getTokenPolicies() {
        return tokenPolicies;
    }

    public String getClientToken() {
        return clientToken;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public String getAccessor() {
        return accessor;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public List<String> getIdentityPolicies() {
        return identityPolicies;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }

    public Object getMfaRequirement() {
        return mfaRequirement;
    }

    public boolean isOrphan() {
        return orphan;
    }

    public String getEntityId() {
        return entityId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenRenewAuth tokenRenewAuth;

        private Builder() {
            tokenRenewAuth = new TokenRenewAuth();
        }

        @JsonCreator
        public static TokenRenewAuth.Builder newInstance() {
            return new TokenRenewAuth.Builder();
        }

        @JsonSetter(value = "token_policies", nulls = Nulls.AS_EMPTY)
        public TokenRenewAuth.Builder tokenPolicies(List<String> tokenPolicies) {
            tokenRenewAuth.tokenPolicies = tokenPolicies;
            return this;
        }

        @JsonProperty("client_token")
        public TokenRenewAuth.Builder clientToken(String clientToken) {
            tokenRenewAuth.clientToken = clientToken;
            return this;
        }

        @JsonSetter(value = "metadata", nulls = Nulls.AS_EMPTY)
        public TokenRenewAuth.Builder metadata(Map<String, String> metadata) {
            tokenRenewAuth.metadata = metadata;
            return this;
        }

        @JsonProperty("renewable")
        public TokenRenewAuth.Builder renewable(boolean renewable) {
            tokenRenewAuth.renewable = renewable;
            return this;
        }

        @JsonProperty("accessor")
        public TokenRenewAuth.Builder accessor(String accessor) {
            tokenRenewAuth.accessor = accessor;
            return this;
        }

        @JsonSetter(value = "policies", nulls = Nulls.AS_EMPTY)
        public TokenRenewAuth.Builder policies(List<String> policies) {
            tokenRenewAuth.policies = policies;
            return this;
        }

        @JsonSetter(value = "identity_policies", nulls = Nulls.AS_EMPTY)
        public TokenRenewAuth.Builder identityPolicies(List<String> identityPolicies) {
            tokenRenewAuth.identityPolicies = identityPolicies;
            return this;
        }

        @JsonProperty("lease_duration")
        public TokenRenewAuth.Builder ttl(long ttl) {
            tokenRenewAuth.leaseDuration = ttl;
            return this;
        }

        @JsonProperty("mfa_requirement")
        public TokenRenewAuth.Builder mfaRequirement(Object mfaRequirement) {
            tokenRenewAuth.mfaRequirement = mfaRequirement;
            return this;
        }

        @JsonProperty("orphan")
        public TokenRenewAuth.Builder orphan(boolean orphan) {
            tokenRenewAuth.orphan = orphan;
            return this;
        }

        @JsonProperty("entity_id")
        public TokenRenewAuth.Builder entityId(String entityId) {
            tokenRenewAuth.entityId = entityId;
            return this;
        }

        public TokenRenewAuth build() {
            return tokenRenewAuth;
        }
    }
}