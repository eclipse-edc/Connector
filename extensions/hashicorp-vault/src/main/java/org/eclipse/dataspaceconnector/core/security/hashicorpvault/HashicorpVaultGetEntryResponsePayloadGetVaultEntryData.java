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
@JsonDeserialize(builder = HashicorpVaultGetEntryResponsePayloadGetVaultEntryData.Builder.class)
class HashicorpVaultGetEntryResponsePayloadGetVaultEntryData {

    @JsonProperty()
    private Map<String, String> data;

    @JsonProperty()
    private HashicorpVaultEntryMetadata metadata;

    HashicorpVaultGetEntryResponsePayloadGetVaultEntryData(Map<String, String> data, HashicorpVaultEntryMetadata metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public Map<String, String> getData() {
        return this.data;
    }

    public HashicorpVaultEntryMetadata getMetadata() {
        return this.metadata;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Map<String, String> data;
        private HashicorpVaultEntryMetadata metadata;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty()
        public Builder data(Map<String, String> data) {
            this.data = data;
            return this;
        }

        @JsonProperty()
        public Builder metadata(HashicorpVaultEntryMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public HashicorpVaultGetEntryResponsePayloadGetVaultEntryData build() {
            return new HashicorpVaultGetEntryResponsePayloadGetVaultEntryData(data, metadata);
        }
    }
}
