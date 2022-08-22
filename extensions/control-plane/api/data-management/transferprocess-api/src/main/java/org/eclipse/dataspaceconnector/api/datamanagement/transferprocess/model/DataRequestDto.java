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

package org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DataRequestDto.Builder.class)
public class DataRequestDto {
    private String assetId;
    private String contractId;
    private String connectorId;

    private DataRequestDto() {
    }

    public String getAssetId() {
        return assetId;
    }

    public String getContractId() {
        return contractId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private final DataRequestDto dataRequestDto;

        private Builder() {
            dataRequestDto = new DataRequestDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            dataRequestDto.assetId = assetId;
            return this;
        }

        public Builder contractId(String contractId) {
            dataRequestDto.contractId = contractId;
            return this;
        }

        public Builder connectorId(String connectorId) {
            dataRequestDto.connectorId = connectorId;
            return this;
        }

        public DataRequestDto build() {
            return dataRequestDto;
        }
    }
}