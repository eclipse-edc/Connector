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

package org.eclipse.edc.connector.controlplane.transfer.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * This event is raised when the TransferProcess has been started.
 */
@JsonDeserialize(builder = TransferProcessStarted.Builder.class)
public class TransferProcessStarted extends TransferProcessEvent {
    private DataAddress dataAddress;

    private TransferProcessStarted() {
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    @Override
    public String name() {
        return "transfer.process.started";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferProcessEvent.Builder<TransferProcessStarted, Builder> {

        private Builder() {
            super(new TransferProcessStarted());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            event.dataAddress = dataAddress;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
