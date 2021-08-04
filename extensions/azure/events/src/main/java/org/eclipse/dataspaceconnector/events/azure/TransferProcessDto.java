/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.events.azure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

/**
 * Data transfer object for {@link TransferProcess} instances.
 * Generally, we should aim to give out as little information as is necessary, e.g. external apps might not need no know
 * the "stateCount" property of a TP. In other instance we simply cannot give internal information, such as the transferprocess id, out for
 * reasons of security.
 */
@JsonDeserialize(builder = TransferProcessDto.Builder.class)
public class TransferProcessDto extends EventDto {
    @JsonProperty("requestId")
    private String requestId;
    @JsonProperty("transferProcessType")
    private TransferProcess.Type type;
    @JsonProperty("transferProcessState")
    private String state;
    @JsonProperty("transferProcessStateCode")
    private int stateCode;

    private TransferProcessDto(String connectorId) {

        super(connectorId);
    }

    public String getRequestId() {
        return requestId;
    }

    public TransferProcess.Type getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public int getStateCode() {
        return stateCode;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String requestId;
        private TransferProcess.Type type;
        private String state;
        private int stateCode;
        private String connectorId;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder type(TransferProcess.Type type) {
            this.type = type;
            return this;
        }

        public Builder state(TransferProcessStates state) {
            this.state = state.toString();
            stateCode = state.code();
            return this;
        }


        public TransferProcessDto build() {
            TransferProcessDto transferProcessDto = new TransferProcessDto(connectorId);
            transferProcessDto.state = state;
            transferProcessDto.type = type;
            transferProcessDto.requestId = requestId;
            transferProcessDto.stateCode = stateCode;
            return transferProcessDto;
        }

        public Builder connector(String connectorId) {
            this.connectorId = connectorId;
            return this;
        }
    }
}
