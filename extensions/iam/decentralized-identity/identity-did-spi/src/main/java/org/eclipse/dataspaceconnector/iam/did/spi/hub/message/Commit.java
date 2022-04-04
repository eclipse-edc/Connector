/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.spi.hub.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;


@JsonDeserialize(builder = Commit.Builder.class)
public class Commit {
    private String alg = "RS256";
    private String kid;
    private InterfaceType interfaze = InterfaceType.Collections;
    private String context;
    private String type;
    private Operation operation = Operation.create;
    private String sub;
    private String committedAt;
    private String objectId;
    private CommitStrategy commitStrategy = CommitStrategy.basic;
    private String rev;
    private String iss;
    private Object payload;

    private Commit() {
    }

    public String getAlg() {
        return alg;
    }

    public String getKid() {
        return kid;
    }

    @JsonProperty("interface")
    public InterfaceType getInterface() {
        return interfaze;
    }

    public String getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    @JsonIgnore
    public String getQualifiedType() {
        return context + ":" + type;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getSub() {
        return sub;
    }

    @JsonProperty("committed_at")
    public String getCommittedAt() {
        return committedAt;
    }

    @JsonProperty("object_id")
    public String getObjectId() {
        return objectId;
    }

    @JsonProperty("commit_strategy")
    public CommitStrategy getCommitStrategy() {
        return commitStrategy;
    }

    public String getRev() {
        return rev;
    }

    public String getIss() {
        return iss;
    }

    public Object getPayload() {
        return payload;
    }

    public enum Operation {
        create, update, delete
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final Commit commit;

        private Builder() {
            commit = new Commit();
        }

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder alg(String alg) {
            commit.alg = alg;
            return this;
        }

        public Builder kid(String kid) {
            commit.kid = kid;
            return this;
        }

        @JsonProperty("interface")
        public Builder interrfaceType(InterfaceType interfaze) {
            commit.interfaze = interfaze;
            return this;
        }

        public Builder context(String context) {
            commit.context = context;
            return this;
        }

        public Builder type(String type) {
            commit.type = type;
            return this;
        }

        public Builder operation(Operation operation) {
            commit.operation = operation;
            return this;
        }

        public Builder sub(String sub) {
            commit.sub = sub;
            return this;
        }

        @JsonProperty("committed_at")
        public Builder committedAt(String committedAt) {
            commit.committedAt = committedAt;
            return this;
        }

        @JsonProperty("object_id")
        public Builder objectId(String objectId) {
            commit.objectId = objectId;
            return this;
        }

        @JsonProperty("commit_strategy")
        public Builder commitStrategy(CommitStrategy commitStrategy) {
            commit.commitStrategy = commitStrategy;
            return this;
        }

        public Builder rev(String rev) {
            commit.rev = rev;
            return this;
        }

        public Builder iss(String iss) {
            commit.iss = iss;
            return this;
        }

        public Builder payload(Object payload) {
            commit.payload = payload;
            return this;
        }

        public Commit build() {
            Objects.requireNonNull(commit.objectId, "objectId");
            Objects.requireNonNull(commit.type, "type");
            Objects.requireNonNull(commit.sub, "sub");
            return commit;
        }
    }


}
