/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = HashicorpVaultEntryMetadata.Builder.class)
class HashicorpVaultEntryMetadata {

    @JsonProperty("custom_metadata")
    private Map<String, String> customMetadata;

    @JsonProperty("destroyed")
    private Boolean destroyed;

    @JsonProperty("version")
    private Integer version;

    HashicorpVaultEntryMetadata(Map<String, String> customMetadata, Boolean destroyed, Integer version) {
        this.customMetadata = customMetadata;
        this.destroyed = destroyed;
        this.version = version;
    }

    public Map<String, String> getCustomMetadata() {
        return this.customMetadata;
    }

    public Boolean getDestroyed() {
        return this.destroyed;
    }

    public Integer getVersion() {
        return this.version;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Map<String, String> customMetadata;
        private Boolean destroyed;
        private Integer version;

        Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("custom_metadata")
        public Builder customMetadata(Map<String, String> customMetadata) {
            this.customMetadata = customMetadata;
            return this;
        }

        @JsonProperty("destroyed")
        public Builder destroyed(Boolean destroyed) {
            this.destroyed = destroyed;
            return this;
        }

        @JsonProperty("version")
        public Builder version(Integer version) {
            this.version = version;
            return this;
        }

        public HashicorpVaultEntryMetadata build() {
            return new HashicorpVaultEntryMetadata(customMetadata, destroyed, version);
        }
    }
}
