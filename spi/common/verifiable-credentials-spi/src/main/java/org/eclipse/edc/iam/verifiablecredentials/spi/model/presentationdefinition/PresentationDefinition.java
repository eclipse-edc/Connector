/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a <a href="https://identity.foundation/presentation-exchange/spec/v2.0.0/#presentation-definition">DIF Presentation Definition</a>
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresentationDefinition {
    private String id;
    private String name;
    private String purpose;
    @JsonProperty("input_descriptors")
    private List<InputDescriptor> inputDescriptors;
    private Map<String, Object> format;

    private PresentationDefinition() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPurpose() {
        return purpose;
    }

    public List<InputDescriptor> getInputDescriptors() {
        return inputDescriptors;
    }

    public Object getFormat() {
        return format;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final PresentationDefinition presentationDefinition;

        private Builder() {
            presentationDefinition = new PresentationDefinition();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.presentationDefinition.id = id;
            return this;
        }

        public Builder name(String name) {
            this.presentationDefinition.name = name;
            return this;
        }

        public Builder purpose(String purpose) {
            this.presentationDefinition.purpose = purpose;
            return this;
        }

        public Builder inputDescriptors(List<InputDescriptor> inputDescriptor) {
            this.presentationDefinition.inputDescriptors = inputDescriptor;
            return this;
        }

        public Builder format(Map<String, Object> format) {
            this.presentationDefinition.format = format;
            return this;
        }

        public PresentationDefinition build() {
            Objects.requireNonNull(presentationDefinition.id, "PresentationDefinition must have an ID.");
            return presentationDefinition;
        }
    }
}
