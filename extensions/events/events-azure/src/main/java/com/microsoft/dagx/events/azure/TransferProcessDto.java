/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.events.azure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates;

/**
 * Data transfer object for {@link TransferProcess} instances.
 * Generally, we should aim to give out as little information as is necessary, e.g. external apps might not need no know
 * the "stateCount" property of a TP. In other instance we simply cannot give internal information, such as the transferprocess id, out for
 * reasons of security.
 */
@JsonDeserialize(builder = TransferProcessDto.Builder.class)
public class TransferProcessDto {
    private String requestId;
    private TransferProcess.Type type;
    private String state;
    private int stateCode;

    private TransferProcessDto() {

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
            TransferProcessDto transferProcessDto = new TransferProcessDto();
            transferProcessDto.state = state;
            transferProcessDto.type = type;
            transferProcessDto.requestId = requestId;
            transferProcessDto.stateCode = stateCode;
            return transferProcessDto;
        }
    }
}
