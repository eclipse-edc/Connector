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
class HashicorpVaultEntryMetadata {

  @JsonProperty("custom_metadata")
  private Map<String, String> customMetadata;

  @JsonProperty("destroyed")
  private Boolean destroyed;

  @JsonProperty("version")
  private Integer version;

    public HashicorpVaultEntryMetadata(Map<String, String> customMetadata, Boolean destroyed, Integer version) {
        this.customMetadata = customMetadata;
        this.destroyed = destroyed;
        this.version = version;
    }

    public HashicorpVaultEntryMetadata() {
    }

    public static HashicorpVaultEntryMetadataBuilder builder() {
        return new HashicorpVaultEntryMetadataBuilder();
    }

    public Map<String, String> getCustomMetadata() {
        return this.customMetadata;
    }

    public Boolean getDestroyed() {
        return this.destroyed;
    }

    public Integer getVersion() {
        return this.version;
    }

    @JsonProperty("custom_metadata")
    public void setCustomMetadata(Map<String, String> customMetadata) {
        this.customMetadata = customMetadata;
    }

    @JsonProperty("destroyed")
    public void setDestroyed(Boolean destroyed) {
        this.destroyed = destroyed;
    }

    @JsonProperty("version")
    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HashicorpVaultEntryMetadata))
            return false;
        final HashicorpVaultEntryMetadata other = (HashicorpVaultEntryMetadata) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$customMetadata = this.getCustomMetadata();
        final Object other$customMetadata = other.getCustomMetadata();
        if (!Objects.equals(this$customMetadata, other$customMetadata))
            return false;
        final Object this$destroyed = this.getDestroyed();
        final Object other$destroyed = other.getDestroyed();
        if (!Objects.equals(this$destroyed, other$destroyed)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        return Objects.equals(this$version, other$version);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HashicorpVaultEntryMetadata;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $customMetadata = this.getCustomMetadata();
        result = result * PRIME + ($customMetadata == null ? 43 : $customMetadata.hashCode());
        final Object $destroyed = this.getDestroyed();
        result = result * PRIME + ($destroyed == null ? 43 : $destroyed.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        return result;
    }

    public String toString() {
        return "HashicorpVaultEntryMetadata(customMetadata=" + this.getCustomMetadata() + ", destroyed=" + this.getDestroyed() + ", version=" + this.getVersion() + ")";
    }

    public static class HashicorpVaultEntryMetadataBuilder {
        private Map<String, String> customMetadata;
        private Boolean destroyed;
        private Integer version;

        HashicorpVaultEntryMetadataBuilder() {
        }

        public HashicorpVaultEntryMetadataBuilder customMetadata(Map<String, String> customMetadata) {
            this.customMetadata = customMetadata;
            return this;
        }

        public HashicorpVaultEntryMetadataBuilder destroyed(Boolean destroyed) {
            this.destroyed = destroyed;
            return this;
        }

        public HashicorpVaultEntryMetadataBuilder version(Integer version) {
            this.version = version;
            return this;
        }

        public HashicorpVaultEntryMetadata build() {
            return new HashicorpVaultEntryMetadata(customMetadata, destroyed, version);
        }

        public String toString() {
            return "HashicorpVaultEntryMetadata.HashicorpVaultEntryMetadataBuilder(customMetadata=" + this.customMetadata + ", destroyed=" + this.destroyed + ", version=" + this.version + ")";
        }
    }
}
