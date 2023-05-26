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
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = TransferRequestDto.Builder.class)
public class TransferRequestDto {

    public static final String EDC_TRANSFER_REQUEST_DTO_TYPE = EDC_NAMESPACE + "TransferRequestDto";
    public static final String EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ADDRESS = EDC_NAMESPACE + "connectorAddress";
    public static final String EDC_TRANSFER_REQUEST_DTO_CONTRACT_ID = EDC_NAMESPACE + "contractId";
    public static final String EDC_TRANSFER_REQUEST_DTO_DATA_DESTINATION = EDC_NAMESPACE + "dataDestination";
    public static final String EDC_TRANSFER_REQUEST_DTO_MANAGED_RESOURCES = EDC_NAMESPACE + "managedResources";
    public static final String EDC_TRANSFER_REQUEST_DTO_PROPERTIES = EDC_NAMESPACE + "properties";

    public static final String EDC_TRANSFER_REQUEST_DTO_PRIVATE_PROPERTIES = EDC_NAMESPACE + "privateProperties";

    public static final String EDC_TRANSFER_REQUEST_DTO_PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String EDC_TRANSFER_REQUEST_DTO_CONNECTOR_ID = EDC_NAMESPACE + "connectorId";
    public static final String EDC_TRANSFER_REQUEST_DTO_ASSET_ID = EDC_NAMESPACE + "assetId";

    private String id;
    @NotNull(message = "connectorAddress cannot be null")
    private String connectorAddress; // TODO change to callbackAddress
    @NotNull(message = "contractId cannot be null")
    private String contractId;
    @NotNull(message = "dataDestination cannot be null")
    private DataAddress dataDestination;
    private boolean managedResources = true;
    private Map<String, String> properties = new HashMap<>();

    private Map<String, String> privateProperties = new HashMap<>();

    @NotNull(message = "protocol cannot be null")
    private String protocol;
    @NotNull(message = "connectorId cannot be null")
    private String connectorId;
    @NotNull(message = "assetId cannot be null")
    private String assetId;

    private List<CallbackAddressDto> callbackAddresses = new ArrayList<>();


    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getId() {
        return id;
    }

    public String getContractId() {
        return contractId;
    }

    public DataAddress getDataDestination() {
        return dataDestination;
    }

    public boolean isManagedResources() {
        return managedResources;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getPrivateProperties() {
        return privateProperties;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getAssetId() {
        return assetId;
    }

    public List<CallbackAddressDto> getCallbackAddresses() {
        return callbackAddresses;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TransferRequestDto request;

        private Builder() {
            request = new TransferRequestDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            request.connectorAddress = connectorAddress;
            return this;
        }

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder contractId(String contractId) {
            request.contractId = contractId;
            return this;
        }

        public Builder dataDestination(DataAddress dataDestination) {
            request.dataDestination = dataDestination;
            return this;
        }

        public Builder managedResources(boolean managedResources) {
            request.managedResources = managedResources;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            request.properties = properties;
            return this;
        }

        public Builder privateProperties(Map<String, String> privateProperties) {
            request.privateProperties = privateProperties;
            return this;
        }

        public Builder protocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            request.connectorId = connectorId;
            return this;
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddressDto> callbackAddresses) {
            request.callbackAddresses = callbackAddresses;
            return this;
        }

        public TransferRequestDto build() {
            return request;
        }
    }
}
