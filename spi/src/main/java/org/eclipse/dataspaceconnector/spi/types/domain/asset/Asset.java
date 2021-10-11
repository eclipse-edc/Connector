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

import java.util.Map;

// TODO maybe create simple class hierarchy for the asset types (file, api, etc.)
// Alternatively use composition over inheritance?

/**
 * The {@link Asset} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Asset.Builder.class)
public class Asset {
    private String id;
    private String name;
    private String description;
    private String version;
    private Integer byteSize;
    private Map<String, String> labels;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }


    public Integer getByteSize() {
        return byteSize;
    }


    public Map<String, String> getLabels() {
        return labels;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String id;
        private String title;
        private String description;
        private String version;
        private Integer byteSize;
        private Map<String, String> labels;

        private Builder() {
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
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }


        public Builder byteSize(Integer byteSize) {
            this.byteSize = byteSize;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Asset build() {
            Asset asset = new Asset();
            asset.id = id;
            asset.name = title;
            asset.description = description;
            asset.version = version;
            asset.byteSize = byteSize;
            asset.labels = labels;
            return asset;
        }
    }
}