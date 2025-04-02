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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputDescriptor {
    private String id;
    private String name;
    private String purpose;
    private Map<String, Object> format;
    private Constraints constraints;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPurpose() {
        return purpose;
    }

    public Object getFormat() {
        return format;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public static final class Builder {
        private final InputDescriptor descriptor;

        private Builder() {
            descriptor = new InputDescriptor();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.descriptor.id = id;
            return this;
        }

        public Builder name(String name) {
            this.descriptor.name = name;
            return this;
        }

        public Builder purpose(String purpose) {
            this.descriptor.purpose = purpose;
            return this;
        }

        public Builder format(Map<String, Object> format) {
            this.descriptor.format = format;
            return this;
        }

        public Builder constraints(Constraints constraints) {
            this.descriptor.constraints = constraints;
            return this;
        }

        public InputDescriptor build() {
            Objects.requireNonNull(descriptor.id, "InputDescriptor must have an ID.");
            Objects.requireNonNull(descriptor.constraints, "InputDescriptor must have a Constraints object.");
            return descriptor;
        }
    }
}
