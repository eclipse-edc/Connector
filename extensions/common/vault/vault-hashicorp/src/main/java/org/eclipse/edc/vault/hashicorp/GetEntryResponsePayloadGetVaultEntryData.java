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

class GetEntryResponsePayloadGetVaultEntryData {

    private Map<String, String> data;

    private EntryMetadata metadata;

    GetEntryResponsePayloadGetVaultEntryData() {}

    public Map<String, String> getData() {
        return this.data;
    }

    public EntryMetadata getMetadata() {
        return this.metadata;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final GetEntryResponsePayloadGetVaultEntryData getEntryResponsePayloadGetVaultEntryData;

        private Builder() {
            getEntryResponsePayloadGetVaultEntryData = new GetEntryResponsePayloadGetVaultEntryData();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder data(Map<String, String> data) {
            getEntryResponsePayloadGetVaultEntryData.data = data;
            return this;
        }

        public Builder metadata(EntryMetadata metadata) {
            getEntryResponsePayloadGetVaultEntryData.metadata = metadata;
            return this;
        }

        public GetEntryResponsePayloadGetVaultEntryData build() {
            return getEntryResponsePayloadGetVaultEntryData;
        }
    }
}
