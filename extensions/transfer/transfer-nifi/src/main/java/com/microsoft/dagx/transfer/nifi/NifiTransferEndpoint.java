/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.Polymorphic;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("dagx:nifitransferendpoint")
@JsonDeserialize(builder = NifiTransferEndpoint.NifiTransferEndpointBuilder.class)
public class NifiTransferEndpoint implements Polymorphic {
    private final String type;
    private Map<String, String> properties;

    protected NifiTransferEndpoint(@JsonProperty("type") String type) {
        this.type = type;
        properties = new HashMap<>();
    }

    public String getType() {
        return type;
    }

    @JsonAnyGetter
    public Map<String, String> getProperties() {
        return properties;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class NifiTransferEndpointBuilder {
        private final Map<String, String> properties;
        private String type;

        private NifiTransferEndpointBuilder() {
            properties = new HashMap<>();
        }

        @JsonCreator
        public static NifiTransferEndpointBuilder newInstance() {
            return new NifiTransferEndpointBuilder();
        }


        public NifiTransferEndpointBuilder type(String type) {
            this.type = type;
            return this;
        }

        @JsonAnySetter
        public NifiTransferEndpointBuilder properties(Map<String, String> additionalProperties) {
            properties.putAll(additionalProperties);
            return this;
        }

        @JsonAnySetter
        public NifiTransferEndpointBuilder property(String key, String value) {
            properties.put(key, value);
            return this;
        }

        public NifiTransferEndpoint build() {
            NifiTransferEndpoint nifiTransferEndpoint = new NifiTransferEndpoint(type);
            nifiTransferEndpoint.properties = properties;
            return nifiTransferEndpoint;
        }
    }
}
