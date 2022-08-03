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
@JsonDeserialize(builder = GetEntryResponsePayload.Builder.class)
class GetEntryResponsePayload {

    @JsonProperty()
    private GetEntryResponsePayloadGetVaultEntryData data;

    GetEntryResponsePayload(GetEntryResponsePayloadGetVaultEntryData data) {
        this.data = data;
    }

    GetEntryResponsePayload() {
    }

    public GetEntryResponsePayloadGetVaultEntryData getData() {
        return this.data;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private GetEntryResponsePayloadGetVaultEntryData data;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty()
        public Builder data(GetEntryResponsePayloadGetVaultEntryData data) {
            this.data = data;
            return this;
        }

        public GetEntryResponsePayload build() {
            return new GetEntryResponsePayload(data);
        }
    }
}
