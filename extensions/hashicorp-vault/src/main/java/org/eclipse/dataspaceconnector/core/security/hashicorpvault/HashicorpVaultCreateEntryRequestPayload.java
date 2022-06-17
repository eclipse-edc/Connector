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
class HashicorpVaultCreateEntryRequestPayload {

    @JsonProperty("options")
    private Options options;

    @JsonProperty("data")
    private Map<String, String> data;

    public HashicorpVaultCreateEntryRequestPayload(Options options, Map<String, String> data) {
        this.options = options;
        this.data = data;
    }

    public HashicorpVaultCreateEntryRequestPayload() {
    }

    public static HashicorpVaultCreateEntryRequestPayloadBuilder builder() {
        return new HashicorpVaultCreateEntryRequestPayloadBuilder();
    }

    public Options getOptions() {
        return this.options;
    }

    @JsonProperty("options")
    public void setOptions(Options options) {
        this.options = options;
    }

    public Map<String, String> getData() {
        return this.data;
    }

    @JsonProperty("data")
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public String toString() {
        return "HashicorpVaultCreateEntryRequestPayload(options=" + this.getOptions() + ", data=" + this.getData() + ")";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Options {
        @JsonProperty("cas")
        private Integer cas;

        public Options(Integer cas) {
            this.cas = cas;
        }

        public Options() {
        }

        public static OptionsBuilder builder() {
            return new OptionsBuilder();
        }

        public Integer getCas() {
            return this.cas;
        }

        @JsonProperty("cas")
        public void setCas(Integer cas) {
            this.cas = cas;
        }

        public String toString() {
            return "HashicorpVaultCreateEntryRequestPayload.Options(cas=" + this.getCas() + ")";
        }

        public static class OptionsBuilder {
            private Integer cas;

            OptionsBuilder() {
            }

            public OptionsBuilder cas(Integer cas) {
                this.cas = cas;
                return this;
            }

            public Options build() {
                return new Options(cas);
            }

            public String toString() {
                return "HashicorpVaultCreateEntryRequestPayload.Options.OptionsBuilder(cas=" + this.cas + ")";
            }
        }
    }

    public static class HashicorpVaultCreateEntryRequestPayloadBuilder {
        private Options options;
        private Map<String, String> data;

        HashicorpVaultCreateEntryRequestPayloadBuilder() {
        }

        public HashicorpVaultCreateEntryRequestPayloadBuilder options(Options options) {
            this.options = options;
            return this;
        }

        public HashicorpVaultCreateEntryRequestPayloadBuilder data(Map<String, String> data) {
            this.data = data;
            return this;
        }

        public HashicorpVaultCreateEntryRequestPayload build() {
            return new HashicorpVaultCreateEntryRequestPayload(options, data);
        }

        public String toString() {
            return "HashicorpVaultCreateEntryRequestPayload.HashicorpVaultCreateEntryRequestPayloadBuilder(options=" + this.options + ", data=" + this.data + ")";
        }
    }
}
