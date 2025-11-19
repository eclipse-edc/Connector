/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Mercedes-Benz Tech Innovation GmbH - connector id removal
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class TransferRequest {

    public static final String TRANSFER_REQUEST_TYPE_TERM = "TransferRequest";
    public static final String TRANSFER_REQUEST_TYPE = EDC_NAMESPACE + TRANSFER_REQUEST_TYPE_TERM;
    public static final String TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS = EDC_NAMESPACE + "counterPartyAddress";
    public static final String TRANSFER_REQUEST_CONTRACT_ID = EDC_NAMESPACE + "contractId";
    public static final String TRANSFER_REQUEST_DATA_DESTINATION = EDC_NAMESPACE + "dataDestination";
    public static final String TRANSFER_REQUEST_TRANSFER_TYPE = EDC_NAMESPACE + "transferType";
    public static final String TRANSFER_REQUEST_PRIVATE_PROPERTIES = EDC_NAMESPACE + "privateProperties";
    public static final String TRANSFER_REQUEST_PROTOCOL = EDC_NAMESPACE + "protocol";
    @Deprecated(since = "management-api:v3")
    public static final String TRANSFER_REQUEST_ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String TRANSFER_REQUEST_CALLBACK_ADDRESSES = EDC_NAMESPACE + "callbackAddresses";
    public static final String TRANSFER_REQUEST_DATAPLANE_METADATA = EDC_NAMESPACE + "dataplaneMetadata";

    private String id;
    private String protocol;
    private String counterPartyAddress;
    private String contractId;
    private String transferType;
    private DataAddress dataDestination;
    private Map<String, Object> privateProperties = new HashMap<>();
    private List<CallbackAddress> callbackAddresses = new ArrayList<>();
    private DataplaneMetadata dataplaneMetadata;

    public String getCounterPartyAddress() {
        return counterPartyAddress;
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

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public String getProtocol() {
        return protocol;
    }

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public String getTransferType() {
        return transferType;
    }

    public DataplaneMetadata getDataplaneMetadata() {
        return dataplaneMetadata;
    }

    public static final class Builder {
        private final TransferRequest request;

        private Builder() {
            request = new TransferRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            request.counterPartyAddress = counterPartyAddress;
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

        public Builder transferType(String transferType) {
            request.transferType = transferType;
            return this;
        }

        public Builder dataDestination(DataAddress dataDestination) {
            request.dataDestination = dataDestination;
            return this;
        }

        public Builder privateProperties(Map<String, Object> privateProperties) {
            request.privateProperties = privateProperties;
            return this;
        }

        public Builder protocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            request.callbackAddresses = callbackAddresses;
            return this;
        }

        public Builder dataplaneMetadata(DataplaneMetadata dataplaneMetadata) {
            request.dataplaneMetadata = dataplaneMetadata;
            return this;
        }

        public TransferRequest build() {
            return request;
        }
    }
}
