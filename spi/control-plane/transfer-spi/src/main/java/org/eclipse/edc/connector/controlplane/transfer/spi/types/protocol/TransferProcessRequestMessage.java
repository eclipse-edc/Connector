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
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.Objects;

/**
 * A message for requesting information about an existing transfer process from the counter-party.
 */
public class TransferProcessRequestMessage implements RemoteMessage {
    
    private String transferProcessId;
    private String protocol;
    private String counterPartyAddress;
    private String counterPartyId;
    
    public String getTransferProcessId() {
        return transferProcessId;
    }
    
    @Override
    public String getProtocol() {
        return protocol;
    }
    
    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }
    
    @Override
    public String getCounterPartyId() {
        return counterPartyId;
    }
    
    public static class Builder {
        private final TransferProcessRequestMessage message;
        
        private Builder() {
            this.message = new TransferProcessRequestMessage();
        }
        
        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
        
        public Builder transferProcessId(String transferProcessId) {
            this.message.transferProcessId = transferProcessId;
            return this;
        }
        
        public Builder protocol(String protocol) {
            this.message.protocol = protocol;
            return this;
        }
        
        public Builder counterPartyAddress(String counterPartyAddress) {
            this.message.counterPartyAddress = counterPartyAddress;
            return this;
        }
        
        public Builder counterPartyId(String counterPartyId) {
            this.message.counterPartyId = counterPartyId;
            return this;
        }
        
        public TransferProcessRequestMessage build() {
            Objects.requireNonNull(message.transferProcessId, "transferProcessId");
            Objects.requireNonNull(message.protocol, "protocol");
            
            return message;
        }
    }
}
