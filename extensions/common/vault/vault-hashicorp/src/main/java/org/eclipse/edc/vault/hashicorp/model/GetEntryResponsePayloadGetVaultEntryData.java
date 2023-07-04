/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

public class GetEntryResponsePayloadGetVaultEntryData {

    private Map<String, String> data;

    private EntryMetadata metadata;

    GetEntryResponsePayloadGetVaultEntryData() {
    }

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
