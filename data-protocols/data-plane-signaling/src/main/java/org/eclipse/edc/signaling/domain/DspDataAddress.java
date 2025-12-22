/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = DspDataAddress.Builder.class)
public final class DspDataAddress {

    public static final String DSP_DATA_ADDRESS_ENDPOINT = EDC_NAMESPACE + "endpoint";

    @JsonProperty(TYPE)
    private final String type = "DataAddress";
    private String endpointType;
    private String endpoint;
    private List<EndpointProperty> endpointProperties = new ArrayList<>();

    private DspDataAddress() {

    }

    public String getEndpointType() {
        return endpointType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public List<EndpointProperty> getEndpointProperties() {
        return endpointProperties;
    }

    public static final class EndpointProperty {
        @JsonProperty(TYPE)
        private final String type = "EndpointProperty";
        private final String name;
        private final String value;

        public EndpointProperty(
                @JsonProperty("name") String name,
                @JsonProperty("value") String value
        ) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final DspDataAddress instance = new DspDataAddress();

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public DspDataAddress build() {
            return instance;
        }

        public Builder endpointType(String endpointType) {
            instance.endpointType = endpointType;
            return this;
        }

        public Builder endpoint(String endpoint) {
            instance.endpoint = endpoint;
            return this;
        }

        public Builder endpointProperties(List<EndpointProperty> endpointProperties) {
            instance.endpointProperties = endpointProperties;
            return this;
        }

        public Builder property(String name, String value) {
            instance.endpointProperties.add(new EndpointProperty(name, value));
            return this;
        }
    }
}
