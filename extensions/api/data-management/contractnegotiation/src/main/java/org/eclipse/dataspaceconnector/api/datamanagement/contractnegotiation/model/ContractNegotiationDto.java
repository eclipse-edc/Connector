/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation.Type;

@JsonDeserialize(builder = ContractNegotiationDto.Builder.class)
public class ContractNegotiationDto {
    private String contractAgreementId; // is null until state == CONFIRMED
    private String counterPartyAddress;
    private String errorDetail;
    private String id;
    private String protocol = "ids-multipart";
    private String state;
    private Type type = Type.CONSUMER;

    private ContractNegotiationDto() {
    }

    public String getId() {
        return id;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public Type getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public String getContractAgreementId() {
        return contractAgreementId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final ContractNegotiationDto dto;

        private Builder() {
            dto = new ContractNegotiationDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            dto.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            dto.protocol = protocol;
            return this;
        }

        public Builder state(String state) {
            dto.state = state;
            return this;
        }

        public Builder errorDetail(String errorDetail) {
            dto.errorDetail = errorDetail;
            return this;
        }

        public Builder contractAgreementId(String contractAgreementId) {
            dto.contractAgreementId = contractAgreementId;
            return this;
        }

        public Builder type(Type type) {
            dto.type = type;
            return this;
        }

        public ContractNegotiationDto build() {
            return dto;
        }
    }
}
