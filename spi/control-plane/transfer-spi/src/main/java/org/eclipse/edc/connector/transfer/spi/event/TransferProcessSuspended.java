/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.transfer.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the TransferProcess has been suspended.
 */
@JsonDeserialize(builder = TransferProcessSuspended.Builder.class)
public class TransferProcessSuspended extends TransferProcessEvent {

    private String reason;

    private TransferProcessSuspended() {
    }

    @Override
    public String name() {
        return "transfer.process.suspended";
    }

    public String getReason() {
        return reason;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferProcessEvent.Builder<TransferProcessSuspended, Builder> {

        private Builder() {
            super(new TransferProcessSuspended());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder reason(String reason) {
            event.reason = reason;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
