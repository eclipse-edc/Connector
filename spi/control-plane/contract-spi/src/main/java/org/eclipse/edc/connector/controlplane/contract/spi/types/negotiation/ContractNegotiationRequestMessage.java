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

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;

import java.util.Objects;

/**
 * A message for requesting information about an existing negotiation from the counter-party.
 */
public class ContractNegotiationRequestMessage extends ProtocolRemoteMessage {
    
    private String negotiationId;

    public String getNegotiationId() {
        return negotiationId;
    }

    public static class Builder extends ProtocolRemoteMessage.Builder<ContractNegotiationRequestMessage, Builder> {

        private Builder() {
            super(new ContractNegotiationRequestMessage());
        }
        
        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
        
        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ContractNegotiationRequestMessage build() {
            Objects.requireNonNull(message.negotiationId, "negotiationId");
            return super.build();
        }

        public Builder negotiationId(String negotiationId) {
            this.message.negotiationId = negotiationId;
            return this;
        }
    }
}
