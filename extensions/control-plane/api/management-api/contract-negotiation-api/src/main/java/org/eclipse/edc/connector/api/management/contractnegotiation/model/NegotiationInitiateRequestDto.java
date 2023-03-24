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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NegotiationInitiateRequestDto {
    @NotBlank(message = "connectorAddress is mandatory")
    private String connectorAddress;
    @NotBlank(message = "protocol is mandatory")
    private String protocol = "ids-multipart";
    @NotBlank(message = "connectorId is mandatory")
    private String connectorId;
    @NotNull(message = "offer cannot be null")
    private ContractOfferDescription offer;
    @NotNull(message = "provider ID cannot be null")
    private String providerId;
    @NotNull(message = "consumer ID cannot be null")
    private String consumerId;

    private NegotiationInitiateRequestDto() {

    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public ContractOfferDescription getOffer() {
        return offer;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getProviderId() {
        return providerId;
    }

    public static final class Builder {
        private final NegotiationInitiateRequestDto dto;

        private Builder() {
            dto = new NegotiationInitiateRequestDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            dto.connectorAddress = connectorAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            dto.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            dto.connectorId = connectorId;
            return this;
        }

        public Builder offer(ContractOfferDescription offer) {
            dto.offer = offer;
            return this;
        }

        public Builder consumerId(String consumerId) {
            dto.consumerId = consumerId;
            return this;
        }

        public Builder providerId(String providerId) {
            dto.providerId = providerId;
            return this;
        }

        public NegotiationInitiateRequestDto build() {
            return dto;
        }
    }
}
