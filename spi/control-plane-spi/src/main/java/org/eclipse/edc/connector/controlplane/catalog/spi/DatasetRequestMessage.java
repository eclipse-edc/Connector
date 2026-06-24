/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;

import java.util.Objects;

/**
 * A request for a participant's {@link Dataset}.
 */
public class DatasetRequestMessage extends ProcessRemoteMessage {

    private String datasetId;
    private final Policy policy;

    private DatasetRequestMessage() {
        // at this time, this is just a placeholder.
        policy = Policy.Builder.newInstance().build();
    }

    public String getDatasetId() {
        return datasetId;
    }

    /**
     * Returns the {@link Policy} associated with the Dataset Request. Currently, this is an empty policy and serves as
     * placeholder.
     *
     * @return the stub {@link Policy}.
     */
    public Policy getPolicy() {
        return policy;
    }

    public static class Builder extends ProcessRemoteMessage.Builder<DatasetRequestMessage, Builder> {

        private Builder() {
            super(new DatasetRequestMessage());
        }

        @JsonCreator
        public static DatasetRequestMessage.Builder newInstance() {
            return new DatasetRequestMessage.Builder();
        }

        public Builder datasetId(String datasetId) {
            this.message.datasetId = datasetId;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public DatasetRequestMessage build() {
            Objects.requireNonNull(message.protocol, "protocol");

            return message;
        }

    }
}
