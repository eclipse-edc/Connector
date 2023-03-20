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

package org.eclipse.edc.spi.event.transferprocess;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when the TransferProcess has been terminated.
 */
@JsonDeserialize(builder = TransferProcessTerminated.Builder.class)
public class TransferProcessTerminated extends TransferProcessEvent {

    private String reason;

    private TransferProcessTerminated() {
    }

    public String getReason() {
        return reason;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferProcessEvent.Builder<TransferProcessTerminated, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder reason(String reason) {
            event.reason = reason;
            return this;
        }

        private Builder() {
            super(new TransferProcessTerminated());
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
