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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;

import java.util.Objects;

/**
 * A message for requesting information about an existing transfer process from the counter-party.
 */
public class TransferProcessRequestMessage extends ProtocolRemoteMessage {
    
    private String transferProcessId;

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public static class Builder extends ProtocolRemoteMessage.Builder<TransferProcessRequestMessage, Builder> {

        private Builder() {
            super(new TransferProcessRequestMessage());
        }
        
        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
        
        public Builder transferProcessId(String transferProcessId) {
            message.transferProcessId = transferProcessId;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public TransferProcessRequestMessage build() {
            Objects.requireNonNull(message.transferProcessId, "transferProcessId");

            return message;
        }
    }
}
