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
package org.eclipse.dataspaceconnector.iam.did.hub.spi.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 *
 */
@JsonTypeName(QueryTypes.COMMIT)
@JsonDeserialize(builder = CommitQuery.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
public class CommitQuery {
    private String objectId;

    @JsonProperty("object_id")
    public String getObjectId() {
        return objectId;
    }

    @JsonProperty(value = "@context")
    public String getContext() {
        return HubMessageConstants.SCHEMA;
    }

    @JsonIgnoreProperties(value = {"@context"}, allowGetters = true, ignoreUnknown = true)
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private CommitQuery objectQuery;

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("object_id")
        public Builder objectId(String objectId) {
            objectQuery.objectId = objectId;
            return this;
        }

        public CommitQuery build() {
            Objects.requireNonNull(objectQuery.objectId, "objectId");
            return objectQuery;
        }

        private Builder() {
            objectQuery = new CommitQuery();
        }


    }

}
