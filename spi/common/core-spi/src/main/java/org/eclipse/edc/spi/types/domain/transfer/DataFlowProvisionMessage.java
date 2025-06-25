/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.transfer;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Message that the consumer control plane uses to trigger data flow preparation on the data plane
 */
public class DataFlowProvisionMessage {

    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_SIMPLE_TYPE = "DataFlowProvisionMessage";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE = EDC_NAMESPACE + EDC_DATA_FLOW_PROVISION_MESSAGE_SIMPLE_TYPE;
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_PROCESS_ID = EDC_NAMESPACE + "processId";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_DATASET_ID = EDC_NAMESPACE + "datasetId";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_PARTICIPANT_ID = EDC_NAMESPACE + "participantId";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_DESTINATION = EDC_NAMESPACE + "destination";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_CALLBACK_ADDRESS = EDC_NAMESPACE + "callbackAddress";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_PROPERTIES = EDC_NAMESPACE + "properties";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE = EDC_NAMESPACE + "flowType";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION = EDC_NAMESPACE + "transferTypeDestination";
    public static final String EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_RESPONSE_CHANNEL = EDC_NAMESPACE + "responseChannel";

    private String id;
    private String processId;

    private String assetId;
    private String participantId;
    private String agreementId;

    private DataAddress destination;
    private URI callbackAddress;

    private Map<String, String> properties = new HashMap<>();
    private TransferType transferType;

    public String getId() {
        return id;
    }

    public String getProcessId() {
        return processId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getAssetId() {
        return assetId;
    }

    public DataAddress getDestination() {
        return destination;
    }

    public URI getCallbackAddress() {
        return callbackAddress;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public String getParticipantId() {
        return participantId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public static class Builder {

        private final DataFlowProvisionMessage message;

        private Builder() {
            message = new DataFlowProvisionMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public DataFlowProvisionMessage build() {
            if (message.id == null) {
                message.id = UUID.randomUUID().toString();
            }
            return message;
        }

        public Builder destination(DataAddress destination) {
            message.destination = destination;
            return this;
        }

        public Builder processId(String id) {
            message.processId = id;
            return this;
        }

        public Builder transferType(TransferType transferType) {
            message.transferType = transferType;
            return this;
        }

        public Builder agreementId(String agreementId) {
            message.agreementId = agreementId;
            return this;
        }

        public Builder participantId(String participantId) {
            message.participantId = participantId;
            return this;
        }

        public Builder assetId(String assetId) {
            message.assetId = assetId;
            return this;
        }

        public Builder properties(Map<String, String> value) {
            message.properties = value == null ? null : Map.copyOf(value);
            return this;
        }

        public Builder property(String key, String value) {
            message.properties.put(key, value);
            return this;
        }

        public Builder callbackAddress(URI callbackAddress) {
            message.callbackAddress = callbackAddress;
            return this;
        }
    }
}
