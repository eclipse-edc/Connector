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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

class CreateEntryRequestPayload {

    private CreateEntryRequestPayloadOptions options;

    private Map<String, String> data;

    CreateEntryRequestPayload() {}

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

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder options(CreateEntryRequestPayloadOptions options) {
            createEntryRequestPayload.options = options;
            return this;
        }

        public Builder data(Map<String, String> data) {
            createEntryRequestPayload.data = data;
            return this;
        }

        public CreateEntryRequestPayload build() {
            return createEntryRequestPayload;
        }
    }
}
