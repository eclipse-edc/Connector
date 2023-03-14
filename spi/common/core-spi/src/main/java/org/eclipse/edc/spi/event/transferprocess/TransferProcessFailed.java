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
 * This event is raised when the TransferProcess has failed.
 */
@JsonDeserialize(builder = TransferProcessFailed.Builder.class)
public class TransferProcessFailed extends TransferProcessEvent {

    private TransferProcessFailed() {
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends TransferProcessEvent.Builder<TransferProcessFailed, Builder> {

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new TransferProcessFailed());
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
