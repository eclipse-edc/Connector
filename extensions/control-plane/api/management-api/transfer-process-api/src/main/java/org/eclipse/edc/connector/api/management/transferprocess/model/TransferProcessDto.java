/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.management.transferprocess.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.MutableDto;

@JsonDeserialize(builder = TransferProcessDto.Builder.class)
public class TransferProcessDto extends MutableDto {
    private String id;
    private String type;
    private String state;
    private Long stateTimestamp;
    private String errorDetail;
    private DataRequestDto dataRequest;
    private DataAddressInformationDto dataDestination;

    private TransferProcessDto() {
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public Long getStateTimestamp() {
        return stateTimestamp;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public DataRequestDto getDataRequest() {
        return dataRequest;
    }

    public DataAddressInformationDto getDataDestination() {
        return dataDestination;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends MutableDto.Builder<TransferProcessDto, Builder> {

        private Builder() {
            super(new TransferProcessDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder type(String type) {
            dto.type = type;
            return this;
        }

        public Builder state(String state) {
            dto.state = state;
            return this;
        }

        public Builder stateTimestamp(Long stateTimestamp) {
            dto.stateTimestamp = stateTimestamp;
            return this;
        }

        public Builder errorDetail(String errorDetail) {
            dto.errorDetail = errorDetail;
            return this;
        }

        public Builder dataRequest(DataRequestDto dataRequest) {
            dto.dataRequest = dataRequest;
            return this;
        }

        public Builder dataDestination(DataAddressInformationDto dataDestination) {
            dto.dataDestination = dataDestination;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
