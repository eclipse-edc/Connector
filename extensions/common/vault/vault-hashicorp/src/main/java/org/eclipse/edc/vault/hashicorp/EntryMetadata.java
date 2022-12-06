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

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonDeserialize(builder = EntryMetadata.Builder.class)
class EntryMetadata {

    @JsonProperty()
    private Map<String, String> customMetadata;

    @JsonProperty()
    private Boolean destroyed;

    @JsonProperty()
    private Integer version;

    EntryMetadata() {}

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
        private final EntryMetadata entryMetadata;

        Builder() {
            entryMetadata = new EntryMetadata();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder customMetadata(Map<String, String> customMetadata) {
            entryMetadata.customMetadata = customMetadata;
            return this;
        }

        public Builder destroyed(Boolean destroyed) {
            entryMetadata.destroyed = destroyed;
            return this;
        }

        public Builder version(Integer version) {
            entryMetadata.version = version;
            return this;
        }

        public EntryMetadata build() {
            return entryMetadata;
        }
    }
}
