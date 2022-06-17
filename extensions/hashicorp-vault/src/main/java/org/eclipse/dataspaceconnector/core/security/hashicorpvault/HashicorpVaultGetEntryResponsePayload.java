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
import java.util.Objects;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HashicorpVaultGetEntryResponsePayload))
            return false;
        final HashicorpVaultGetEntryResponsePayload other = (HashicorpVaultGetEntryResponsePayload) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$data = this.getData();
        final Object other$data = other.getData();
        if (!Objects.equals(this$data, other$data)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HashicorpVaultGetEntryResponsePayload;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
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

        public HashicorpVaultEntryMetadata getMetadata() {
            return this.metadata;
        }

        @JsonProperty("data")
        public void setData(Map<String, String> data) {
            this.data = data;
        }

        @JsonProperty("metadata")
        public void setMetadata(HashicorpVaultEntryMetadata metadata) {
            this.metadata = metadata;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof GetVaultEntryData))
                return false;
            final GetVaultEntryData other = (GetVaultEntryData) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$data = this.getData();
            final Object other$data = other.getData();
            if (!Objects.equals(this$data, other$data)) return false;
            final Object this$metadata = this.getMetadata();
            final Object other$metadata = other.getMetadata();
            if (!Objects.equals(this$metadata, other$metadata)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof GetVaultEntryData;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $data = this.getData();
            result = result * PRIME + ($data == null ? 43 : $data.hashCode());
            final Object $metadata = this.getMetadata();
            result = result * PRIME + ($metadata == null ? 43 : $metadata.hashCode());
            return result;
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
