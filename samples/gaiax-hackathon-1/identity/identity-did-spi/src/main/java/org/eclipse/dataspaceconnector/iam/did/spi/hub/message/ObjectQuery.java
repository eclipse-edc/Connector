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
@JsonTypeName(QueryTypes.OBJECT)
@JsonDeserialize(builder = ObjectQuery.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
public class ObjectQuery {
    private InterfaceType interfaze = InterfaceType.Collections;
    private String context;
    private String type;

    public InterfaceType getInterface() {
        return interfaze;
    }

    public String getType() {
        return type;
    }

    public String getContext() {
        return context;
    }

    @JsonIgnore
    public String getQualifiedType() {
        return context + ":" + type;
    }

    @JsonIgnoreProperties(value = {"@context"}, allowGetters = true, ignoreUnknown = true)
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ObjectQuery objectQuery;

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("interface")
        public Builder interfaze(InterfaceType interfaze) {
            objectQuery.interfaze = interfaze;
            return this;
        }

        public Builder context(String context) {
            objectQuery.context = context;
            return this;
        }

        public Builder type(String type) {
            objectQuery.type = type;
            return this;
        }

        public ObjectQuery build() {
            Objects.requireNonNull(objectQuery.type, "type");
            return objectQuery;
        }

        private Builder() {
            objectQuery = new ObjectQuery();
        }


    }

}
