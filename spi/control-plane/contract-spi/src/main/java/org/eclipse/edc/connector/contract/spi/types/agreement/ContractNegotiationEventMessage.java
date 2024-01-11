/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.types.agreement;

import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ContractNegotiationEventMessage extends ContractRemoteMessage {
    private Type type;
    private Policy policy;

    @NotNull
    public Type getType() {
        return type;
    }

    @Override
    public Policy getPolicy() {
        return policy;
    }

    public static class Builder extends ContractRemoteMessage.Builder<ContractNegotiationEventMessage, Builder> {

        private Builder() {
            super(new ContractNegotiationEventMessage());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(Type type) {
            message.type = type;
            return this;
        }

        public Builder policy(Policy policy) {
            message.policy = policy;
            return this;
        }

        public ContractNegotiationEventMessage build() {
            Objects.requireNonNull(message.type, "type");
            return super.build();
        }
    }

    public enum Type {
        ACCEPTED, FINALIZED
    }
}
