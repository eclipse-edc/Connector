/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.domain;

import java.net.URI;
import java.util.List;
import java.util.Map;

public final class DataFlowPrepareMessage {
    private String messageId;
    private String participantId;
    private String counterPartyId;
    private String dataspaceContext;
    private String processId;
    private String agreementId;
    private String datasetId;
    private URI callbackAddress;
    private String transferType;
    private List<String> labels;
    private Map<String, Object> metadata;

    private DataFlowPrepareMessage() {
    }

    public String getMessageId() {
        return messageId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public String getDataspaceContext() {
        return dataspaceContext;
    }

    public String getProcessId() {
        return processId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public URI getCallbackAddress() {
        return callbackAddress;
    }

    public String getTransferType() {
        return transferType;
    }

    public List<String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static class Builder {

        private final DataFlowPrepareMessage instance = new DataFlowPrepareMessage();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {

        }

        public DataFlowPrepareMessage build() {
            return instance;
        }

        public Builder messageId(String messageId) {
            instance.messageId = messageId;
            return this;
        }

        public Builder participantId(String participantId) {
            instance.participantId = participantId;
            return this;
        }

        public Builder counterPartyId(String counterPartyId) {
            instance.counterPartyId = counterPartyId;
            return this;
        }

        public Builder dataspaceContext(String dataspaceContext) {
            instance.dataspaceContext = dataspaceContext;
            return this;
        }

        public Builder processId(String processId) {
            instance.processId = processId;
            return this;
        }

        public Builder agreementId(String agreementId) {
            instance.agreementId = agreementId;
            return this;
        }

        public Builder datasetId(String datasetId) {
            instance.datasetId = datasetId;
            return this;
        }

        public Builder callbackAddress(URI callbackAddress) {
            instance.callbackAddress = callbackAddress;
            return this;
        }

        public Builder transferType(String transferType) {
            instance.transferType = transferType;
            return this;
        }

        public Builder labels(List<String> labels) {
            instance.labels = labels;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            instance.metadata = metadata;
            return this;
        }

    }
}
