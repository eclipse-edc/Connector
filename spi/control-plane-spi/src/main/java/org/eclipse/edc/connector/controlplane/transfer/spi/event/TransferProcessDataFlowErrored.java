/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the data plane reports a non-recoverable error of the data flow of a TransferProcess. As
 * defined by the Data Plane Signaling specification, the TransferProcess doesn't change state and the counter-party is
 * not notified: recovering from the error is up to the operator.
 */
@JsonDeserialize(builder = TransferProcessDataFlowErrored.Builder.class)
public class TransferProcessDataFlowErrored extends TransferProcessEvent {

    private String error;

    private TransferProcessDataFlowErrored() {
    }

    @Override
    public String name() {
        return "transfer.process.dataflow.errored";
    }

    public String getError() {
        return error;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferProcessEvent.Builder<TransferProcessDataFlowErrored, Builder> {

        private Builder() {
            super(new TransferProcessDataFlowErrored());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder error(String error) {
            event.error = error;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
