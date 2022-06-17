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

@JsonIgnoreProperties(ignoreUnknown = true)
class HashicorpVaultCreateEntryResponsePayload {

    @JsonProperty("data")
    private HashicorpVaultEntryMetadata data;

    public HashicorpVaultCreateEntryResponsePayload(HashicorpVaultEntryMetadata data) {
        this.data = data;
    }

    public HashicorpVaultCreateEntryResponsePayload() {
    }

    public static HashicorpVaultCreateEntryResponsePayloadBuilder builder() {
        return new HashicorpVaultCreateEntryResponsePayloadBuilder();
    }

    public HashicorpVaultEntryMetadata getData() {
        return this.data;
    }

    @JsonProperty("data")
    public void setData(HashicorpVaultEntryMetadata data) {
        this.data = data;
    }

    public String toString() {
        return "HashicorpVaultCreateEntryResponsePayload(data=" + this.getData() + ")";
    }

    public static class HashicorpVaultCreateEntryResponsePayloadBuilder {
        private HashicorpVaultEntryMetadata data;

        HashicorpVaultCreateEntryResponsePayloadBuilder() {
        }

        public HashicorpVaultCreateEntryResponsePayloadBuilder data(HashicorpVaultEntryMetadata data) {
            this.data = data;
            return this;
        }

        public HashicorpVaultCreateEntryResponsePayload build() {
            return new HashicorpVaultCreateEntryResponsePayload(data);
        }

        public String toString() {
            return "HashicorpVaultCreateEntryResponsePayload.HashicorpVaultCreateEntryResponsePayloadBuilder(data=" + this.data + ")";
        }
    }
}
