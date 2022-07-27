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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

public class NegotiationInitiateRequestDto {
    @NotNull(message = "connectorAddress cannot be null")
    private String connectorAddress;
    @NotNull(message = "protocol cannot be null")
    private String protocol = "ids-multipart";
    @NotNull(message = "connectorId cannot be null")
    private String connectorId;
    @NotNull(message = "offer cannot be null")
    private ContractOfferDescription offer;

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

    @AssertTrue(message = "connectorId, connectorAddress, protocol, offerId and assetId cannot be blank")
    @JsonIgnore
    public boolean isValid() {
        return this.getOffer() != null && StringUtils.isNoneBlank(
                this.getConnectorId(), this.getConnectorAddress(), this.getProtocol(),
                this.getOffer().getOfferId(), this.getOffer().getAssetId()
        );
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

        public NegotiationInitiateRequestDto build() {
            return dto;
        }
    }
}
