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
import org.eclipse.edc.api.model.BaseDto;
import org.eclipse.edc.api.model.CallbackAddressDto;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class NegotiationInitiateRequestDto extends BaseDto {
    public static final String TYPE = EDC_NAMESPACE + "NegotiationInitiateRequestDto";
    public static final String CONNECTOR_ADDRESS = EDC_NAMESPACE + "connectorAddress";
    public static final String PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String CONNECTOR_ID = EDC_NAMESPACE + "connectorId";
    public static final String PROVIDER_ID = EDC_NAMESPACE + "providerId";
    public static final String CONSUMER_ID = EDC_NAMESPACE + "consumerId";
    public static final String OFFER = EDC_NAMESPACE + "offer";
    public static final String CALLBACK_ADDRESSES = EDC_NAMESPACE + "callbackAddresses";
    public static final String OFFER_ID = EDC_NAMESPACE + "offerId";
    public static final String ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String POLICY = EDC_NAMESPACE + "policy";

    @NotBlank(message = "connectorAddress is mandatory")
    private String connectorAddress; // TODO change to callbackAddress
    @NotBlank(message = "protocol is mandatory")
    private String protocol;
    @NotBlank(message = "connectorId is mandatory")
    private String connectorId;
    @NotNull(message = "offer cannot be null")
    private ContractOfferDescription offer;
    private String providerId;
    private String consumerId;

    private List<CallbackAddressDto> callbackAddresses = new ArrayList<>();

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

    public List<CallbackAddressDto> getCallbackAddresses() {
        return callbackAddresses;
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

        public Builder callbackAddresses(List<CallbackAddressDto> callbackAddresses) {
            dto.callbackAddresses = callbackAddresses;
            return this;
        }

        public NegotiationInitiateRequestDto build() {
            return dto;
        }
    }
}
