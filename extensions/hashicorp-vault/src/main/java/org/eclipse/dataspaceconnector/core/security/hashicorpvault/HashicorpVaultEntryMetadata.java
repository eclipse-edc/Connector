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

    @JsonProperty("custom_metadata")
    public void setCustomMetadata(Map<String, String> customMetadata) {
        this.customMetadata = customMetadata;
    }

    public Boolean getDestroyed() {
        return this.destroyed;
    }

    @JsonProperty("destroyed")
    public void setDestroyed(Boolean destroyed) {
        this.destroyed = destroyed;
    }

    public Integer getVersion() {
        return this.version;
    }

    @JsonProperty("version")
    public void setVersion(Integer version) {
        this.version = version;
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
