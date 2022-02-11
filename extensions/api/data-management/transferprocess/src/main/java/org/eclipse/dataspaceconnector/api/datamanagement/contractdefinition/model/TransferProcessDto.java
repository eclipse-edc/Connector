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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = TransferProcessDto.Builder.class)
public class TransferProcessDto {
    private String id;
    private String type;
    private String state;
    private String errorDetail;
    private DataRequestDto dataRequest;

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

    public String getErrorDetail() {
        return errorDetail;
    }

    public DataRequestDto getDataRequest() {
        return dataRequest;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TransferProcessDto transferProcessDto;

        private Builder() {
            transferProcessDto = new TransferProcessDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            transferProcessDto.id = id;
            return this;
        }

        public Builder type(String type) {
            transferProcessDto.type = type;
            return this;
        }

        public Builder state(String state) {
            transferProcessDto.state = state;
            return this;
        }

        public Builder errorDetail(String errorDetail) {
            transferProcessDto.errorDetail = errorDetail;
            return this;
        }

        public Builder dataRequest(DataRequestDto dataRequest) {
            transferProcessDto.dataRequest = dataRequest;
            return this;
        }

        public TransferProcessDto build() {
            return transferProcessDto;
        }
    }
}
