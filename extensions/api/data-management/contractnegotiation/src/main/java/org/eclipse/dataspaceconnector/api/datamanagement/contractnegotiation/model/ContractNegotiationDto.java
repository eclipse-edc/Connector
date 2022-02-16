/*
 * Copyright (c) 2022 Florian Rusch
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Florian Rusch - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation.Type;

@JsonDeserialize(builder = ContractNegotiationDto.Builder.class)
public class ContractNegotiationDto {
    private String id;
    private String counterPartyAddress;
    private String protocol;
    private final Type type = Type.CONSUMER;
    private String state;
    private String errorDetail;
    private String contractAgreementId; // is null until state == CONFIRMED

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
            if (type != Type.CONSUMER) {
                throw new IllegalArgumentException("The type can only be CONSUMER");
            }
            return this;
        }

        public ContractNegotiationDto build() {
            return dto;
        }
    }
}
