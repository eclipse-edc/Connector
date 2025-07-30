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
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class DatasetRequest {

    public static final String DATASET_REQUEST_TYPE_TERM = "DatasetRequest";
    public static final String DATASET_REQUEST_TYPE = EDC_NAMESPACE + DATASET_REQUEST_TYPE_TERM;
    public static final String DATASET_REQUEST_PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String DATASET_REQUEST_COUNTER_PARTY_ADDRESS = EDC_NAMESPACE + "counterPartyAddress";
    public static final String DATASET_REQUEST_COUNTER_PARTY_ID = EDC_NAMESPACE + "counterPartyId";

    private String id;
    private String counterPartyAddress;
    private String counterPartyId;
    private String protocol;

    private DatasetRequest() {
    }

    public String getId() {
        return id;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public String getProtocol() {
        return protocol;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final DatasetRequest instance;

        private Builder() {
            instance = new DatasetRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            instance.id = id;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            instance.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder counterPartyId(String counterPartyId) {
            instance.counterPartyId = counterPartyId;
            return this;
        }

        public Builder protocol(String protocol) {
            instance.protocol = protocol;
            return this;
        }

        public DatasetRequest build() {
            return instance;
        }
    }
}
