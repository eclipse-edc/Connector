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
@JsonDeserialize(builder = HashicorpVaultCreateEntryRequestPayload.Builder.class)
class HashicorpVaultCreateEntryRequestPayload {

    @JsonProperty()
    private HashicorpVaultCreateEntryRequestPayloadOptions options;

    @JsonProperty()
    private Map<String, String> data;

    HashicorpVaultCreateEntryRequestPayload(HashicorpVaultCreateEntryRequestPayloadOptions options, Map<String, String> data) {
        this.options = options;
        this.data = data;
    }

    public HashicorpVaultCreateEntryRequestPayloadOptions getOptions() {
        return this.options;
    }

    public Map<String, String> getData() {
        return this.data;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private HashicorpVaultCreateEntryRequestPayloadOptions options;
        private Map<String, String> data;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty()
        public Builder options(HashicorpVaultCreateEntryRequestPayloadOptions options) {
            this.options = options;
            return this;
        }

        @JsonProperty()
        public Builder data(Map<String, String> data) {
            this.data = data;
            return this;
        }

        public HashicorpVaultCreateEntryRequestPayload build() {
            return new HashicorpVaultCreateEntryRequestPayload(options, data);
        }
    }
}
