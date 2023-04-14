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

package org.eclipse.edc.protocol.dsp.transferprocess.spi.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;

@JsonTypeName("dspace:TransferError")
@JsonDeserialize(builder = TransferError.Builder.class)
public class TransferError {
    private String processId;

    private String correlationId;

    private String code;

    private List<String> reason;

    @JsonProperty("dspace:processId")
    public String getProcessId() {
        return processId;
    }

    @JsonProperty("dspace:correlationId")
    public String getCorrelationId() {
        return correlationId;
    }

    @JsonProperty("dspace:code")
    public String getCode() {
        return code;
    }

    @JsonProperty("dspace:reason")
    public List<String> getReason() {
        return reason;
    }

    public static class Builder {
        TransferError transferError;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder processId(String processId) {
            transferError.processId = processId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            transferError.correlationId = correlationId;
            return this;
        }

        public Builder code(String code) {
            transferError.code = code;
            return this;
        }

        public Builder reason(List<String> reason) {
            transferError.reason = reason;
            return this;
        }

        public TransferError build() {
            Objects.requireNonNull(transferError.processId, "The processId must be specified");
            Objects.requireNonNull(transferError.correlationId, "The correlationId must be specified");
            Objects.requireNonNull(transferError.code, "The code must be specified");
            Objects.requireNonNull(transferError.reason, "The reason must be specified");

            return transferError;
        }


    }

}
