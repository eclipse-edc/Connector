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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Metadata for an object stored in the identity hub.
 */
@JsonTypeName("HubObject")
@JsonDeserialize(builder = HubObject.Builder.class)
public class HubObject {
    private InterfaceType interfaze = InterfaceType.Collections;
    private String type;
    private String id;
    private String sub;
    private String createdBy;
    private CommitStrategy commitStrategy = CommitStrategy.basic;

    public InterfaceType getInterface() {
        return interfaze;
    }

    @JsonProperty("@type")
    public String getType() {
        return type;
    }

    @JsonProperty(value = "@context")
    public String getContext() {
        return HubMessageConstants.SCHEMA;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("created_by")
    public String getCreatedBy() {
        return createdBy;
    }

    public String getSub() {
        return sub;
    }

    @JsonProperty(value = "commit_strategy")
    public CommitStrategy getCommitStrategy() {
        return commitStrategy;
    }

    private HubObject() {
    }

    @JsonIgnoreProperties(value = {"@context"}, allowGetters = true, ignoreUnknown = true)
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private HubObject hubObject;

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("@type")
        public Builder type(String type) {
            hubObject.type = type;
            return this;
        }

        @JsonProperty("interface")
        public Builder interfaze(InterfaceType interfaze) {
            hubObject.interfaze = interfaze;
            return this;
        }

        public Builder id(String id) {
            hubObject.id = id;
            return this;
        }

        @JsonProperty("created_by")
        public Builder createdBy(String did) {
            hubObject.createdBy = did;
            return this;
        }

        public Builder sub(String sub) {
            this.hubObject.sub = sub;
            return this;
        }

        @JsonProperty("commit_strategy")
        public Builder commitStrategy(CommitStrategy strategy) {
            hubObject.commitStrategy = strategy;
            return this;
        }


        public HubObject build() {
            Objects.requireNonNull(hubObject.type, "type");
            Objects.requireNonNull(hubObject.id, "id");
            Objects.requireNonNull(hubObject.createdBy, "createdBy");
            Objects.requireNonNull(hubObject.sub, "sub");
            return hubObject;
        }

        private Builder() {
            hubObject = new HubObject();
        }


    }


}
