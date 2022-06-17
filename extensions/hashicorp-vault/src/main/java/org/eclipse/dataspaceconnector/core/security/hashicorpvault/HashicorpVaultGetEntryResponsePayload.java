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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
class HashicorpVaultGetEntryResponsePayload {

    @JsonProperty("data")
    private GetVaultEntryData data;

    public HashicorpVaultGetEntryResponsePayload(GetVaultEntryData data) {
        this.data = data;
    }

    public HashicorpVaultGetEntryResponsePayload() {
    }

    public static HashicorpVaultGetEntryResponsePayloadBuilder builder() {
        return new HashicorpVaultGetEntryResponsePayloadBuilder();
    }

    public GetVaultEntryData getData() {
        return this.data;
    }

    @JsonProperty("data")
    public void setData(GetVaultEntryData data) {
        this.data = data;
    }

    public String toString() {
        return "HashicorpVaultGetEntryResponsePayload(data=" + this.getData() + ")";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GetVaultEntryData {

        @JsonProperty("data")
        private Map<String, String> data;

        @JsonProperty("metadata")
        private HashicorpVaultEntryMetadata metadata;

        public GetVaultEntryData(Map<String, String> data, HashicorpVaultEntryMetadata metadata) {
            this.data = data;
            this.metadata = metadata;
        }

        public GetVaultEntryData() {
        }

        public static GetVaultEntryDataBuilder builder() {
            return new GetVaultEntryDataBuilder();
        }

        public Map<String, String> getData() {
            return this.data;
        }

        @JsonProperty("data")
        public void setData(Map<String, String> data) {
            this.data = data;
        }

        public HashicorpVaultEntryMetadata getMetadata() {
            return this.metadata;
        }

        @JsonProperty("metadata")
        public void setMetadata(HashicorpVaultEntryMetadata metadata) {
            this.metadata = metadata;
        }

        public String toString() {
            return "HashicorpVaultGetEntryResponsePayload.GetVaultEntryData(data=" + this.getData() + ", metadata=" + this.getMetadata() + ")";
        }

        public static class GetVaultEntryDataBuilder {
            private Map<String, String> data;
            private HashicorpVaultEntryMetadata metadata;

            GetVaultEntryDataBuilder() {
            }

            public GetVaultEntryDataBuilder data(Map<String, String> data) {
                this.data = data;
                return this;
            }

            public GetVaultEntryDataBuilder metadata(HashicorpVaultEntryMetadata metadata) {
                this.metadata = metadata;
                return this;
            }

            public GetVaultEntryData build() {
                return new GetVaultEntryData(data, metadata);
            }

            public String toString() {
                return "HashicorpVaultGetEntryResponsePayload.GetVaultEntryData.GetVaultEntryDataBuilder(data=" + this.data + ", metadata=" + this.metadata + ")";
            }
        }
    }

    public static class HashicorpVaultGetEntryResponsePayloadBuilder {
        private GetVaultEntryData data;

        HashicorpVaultGetEntryResponsePayloadBuilder() {
        }

        public HashicorpVaultGetEntryResponsePayloadBuilder data(GetVaultEntryData data) {
            this.data = data;
            return this;
        }

        public HashicorpVaultGetEntryResponsePayload build() {
            return new HashicorpVaultGetEntryResponsePayload(data);
        }

        public String toString() {
            return "HashicorpVaultGetEntryResponsePayload.HashicorpVaultGetEntryResponsePayloadBuilder(data=" + this.data + ")";
        }
    }
}
