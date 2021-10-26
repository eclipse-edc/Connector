/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Asset.Builder.class)
public class Asset {
    private final String id;
    private final String name;
    private final String version;
    private final String contentType;
    private final Map<String, Object> properties;

    private Asset(@NotNull String id, @NotNull String name, @NotNull String version, @NotNull String contentType, @NotNull Map<String, Object> properties) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.contentType = contentType;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String id;
        private String name;
        private String version;
        private String contentType;
        private Map<String, Object> properties;

        protected Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String title) {
            this.name = title;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Asset build() {
            return new Asset(id, name, version, contentType, properties);
        }
    }
}
