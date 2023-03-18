/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.controlplane.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Objects;

@JsonTypeName("dspace:ContractNegotiationError")
@JsonDeserialize(builder = ContractNegotiationError.Builder.class)
public class ContractNegotiationError {

    private String processId;

    private String code;

    private List<String> reasons; // TODO should be object?

    @JsonProperty("dspace:processId")
    public String getProcessId() {
        return processId;
    }

    @JsonProperty("dspace:code")
    public String getCode() {
        return code;
    }

    @JsonProperty("dspace:reason")
    public List<String> getReasons() {
        return reasons;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final ContractNegotiationError error;

        private Builder() {
            error = new ContractNegotiationError();
        }

        @JsonCreator
        public static ContractNegotiationError.Builder newInstance() {
            return new ContractNegotiationError.Builder();
        }

        public ContractNegotiationError.Builder processId(String processId) {
            error.processId = processId;
            return this;
        }

        public ContractNegotiationError.Builder code(String code) {
            error.code = code;
            return this;
        }

        public ContractNegotiationError.Builder reasons(List<String> reasons) {
            error.reasons = reasons;
            return this;
        }

        public ContractNegotiationError build() {
            Objects.requireNonNull(error.processId, "The processId must be specified");
            Objects.requireNonNull(error.code, "The code must be specified");

            return error;
        }
    }
}
