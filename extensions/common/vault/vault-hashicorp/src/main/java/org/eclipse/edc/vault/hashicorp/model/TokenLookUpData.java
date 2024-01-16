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
 * Token data returned by the Vault token lookup operation.
 */
@JsonDeserialize(builder = TokenLookUpData.Builder.class)
public class TokenLookUpData {

    private static final String ROOT_POLICY = "root";

    private long creationTime;

    private long creationTtl;

    private String accessor;

    private List<String> policies;

    private String expireTime;

    private int numUses;

    private String displayName;

    private String entityId;

    private boolean orphan;

    private String type;

    private long ttl;

    private long explicitMaxTtl;

    private String path;

    private String period;

    private Map<String, String> meta;

    private boolean renewable;

    private String id;

    private String issueTime;

    private TokenLookUpData() {
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getCreationTtl() {
        return creationTtl;
    }

    public String getAccessor() {
        return accessor;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public int getNumUses() {
        return numUses;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEntityId() {
        return entityId;
    }

    public boolean isOrphan() {
        return orphan;
    }

    public String getType() {
        return type;
    }

    public long getTtl() {
        return ttl;
    }

    public long getExplicitMaxTtl() {
        return explicitMaxTtl;
    }

    public String getPath() {
        return path;
    }

    public String getPeriod() {
        return period;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public String getId() {
        return id;
    }

    public String getIssueTime() {
        return issueTime;
    }

    public boolean isRootToken() {
        return policies.contains(ROOT_POLICY);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final TokenLookUpData tokenLookUpData;

        private Builder() {
            tokenLookUpData = new TokenLookUpData();
        }

        @JsonCreator
        public static TokenLookUpData.Builder newInstance() {
            return new TokenLookUpData.Builder();
        }

        @JsonProperty("creation_time")
        public TokenLookUpData.Builder creationTime(long creationTime) {
            tokenLookUpData.creationTime = creationTime;
            return this;
        }

        @JsonProperty("creation_ttl")
        public TokenLookUpData.Builder creationTtl(long creationTtl) {
            tokenLookUpData.creationTtl = creationTtl;
            return this;
        }

        @JsonProperty("accessor")
        public TokenLookUpData.Builder accessor(String accessor) {
            tokenLookUpData.accessor = accessor;
            return this;
        }

        @JsonSetter(value = "policies", nulls = Nulls.AS_EMPTY)
        public TokenLookUpData.Builder policies(List<String> policies) {
            tokenLookUpData.policies = policies;
            return this;
        }

        @JsonProperty("expire_time")
        public TokenLookUpData.Builder expireTime(String expireTime) {
            tokenLookUpData.expireTime = expireTime;
            return this;
        }

        @JsonProperty("num_uses")
        public TokenLookUpData.Builder numUses(int numUses) {
            tokenLookUpData.numUses = numUses;
            return this;
        }

        @JsonProperty("display_name")
        public TokenLookUpData.Builder displayName(String displayName) {
            tokenLookUpData.displayName = displayName;
            return this;
        }

        @JsonProperty("entity_id")
        public TokenLookUpData.Builder entityId(String entityId) {
            tokenLookUpData.entityId = entityId;
            return this;
        }

        @JsonProperty("orphan")
        public TokenLookUpData.Builder orphan(boolean orphan) {
            tokenLookUpData.orphan = orphan;
            return this;
        }

        @JsonProperty("type")
        public TokenLookUpData.Builder type(String type) {
            tokenLookUpData.type = type;
            return this;
        }

        @JsonProperty("ttl")
        public TokenLookUpData.Builder ttl(long ttl) {
            tokenLookUpData.ttl = ttl;
            return this;
        }

        @JsonProperty("explicit_max_ttl")
        public TokenLookUpData.Builder explicitMaxTtl(long explicitMaxTtl) {
            tokenLookUpData.explicitMaxTtl = explicitMaxTtl;
            return this;
        }

        @JsonProperty("path")
        public TokenLookUpData.Builder path(String path) {
            tokenLookUpData.path = path;
            return this;
        }

        @JsonProperty("period")
        public TokenLookUpData.Builder period(String period) {
            tokenLookUpData.period = period;
            return this;
        }

        @JsonSetter(value = "meta", nulls = Nulls.AS_EMPTY)
        public TokenLookUpData.Builder meta(Map<String, String> meta) {
            tokenLookUpData.meta = meta;
            return this;
        }

        @JsonProperty("renewable")
        public TokenLookUpData.Builder renewable(boolean renewable) {
            tokenLookUpData.renewable = renewable;
            return this;
        }

        @JsonProperty("id")
        public TokenLookUpData.Builder id(String id) {
            tokenLookUpData.id = id;
            return this;
        }

        @JsonProperty("issue_time")
        public TokenLookUpData.Builder issueTime(String issueTime) {
            tokenLookUpData.issueTime = issueTime;
            return this;
        }

        public TokenLookUpData build() {
            return tokenLookUpData;
        }
    }
}