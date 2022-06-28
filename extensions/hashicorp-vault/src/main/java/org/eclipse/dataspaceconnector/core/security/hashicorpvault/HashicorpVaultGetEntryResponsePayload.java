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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = HashicorpVaultGetEntryResponsePayload.Builder.class)
class HashicorpVaultGetEntryResponsePayload {

    @JsonProperty("data")
    private HashicorpVaultGetEntryResponsePayloadGetVaultEntryData data;

    HashicorpVaultGetEntryResponsePayload(HashicorpVaultGetEntryResponsePayloadGetVaultEntryData data) {
        this.data = data;
    }

    HashicorpVaultGetEntryResponsePayload() {
    }

    public HashicorpVaultGetEntryResponsePayloadGetVaultEntryData getData() {
        return this.data;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private HashicorpVaultGetEntryResponsePayloadGetVaultEntryData data;

        Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("data")
        public Builder data(HashicorpVaultGetEntryResponsePayloadGetVaultEntryData data) {
            this.data = data;
            return this;
        }

        public HashicorpVaultGetEntryResponsePayload build() {
            return new HashicorpVaultGetEntryResponsePayload(data);
        }
    }
}
