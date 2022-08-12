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
@JsonDeserialize(builder = CreateEntryRequestPayload.Builder.class)
class CreateEntryRequestPayload {

    @JsonProperty()
    private CreateEntryRequestPayloadOptions options;

    @JsonProperty()
    private Map<String, String> data;

    public CreateEntryRequestPayload() {}

    public CreateEntryRequestPayloadOptions getOptions() {
        return this.options;
    }

    public Map<String, String> getData() {
        return this.data;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final CreateEntryRequestPayload createEntryRequestPayload;

        private Builder() {
            createEntryRequestPayload = new CreateEntryRequestPayload();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty()
        public Builder options(CreateEntryRequestPayloadOptions options) {
            createEntryRequestPayload.options = options;
            return this;
        }

        @JsonProperty()
        public Builder data(Map<String, String> data) {
            createEntryRequestPayload.data = data;
            return this;
        }

        public CreateEntryRequestPayload build() {
            return createEntryRequestPayload;
        }
    }
}
