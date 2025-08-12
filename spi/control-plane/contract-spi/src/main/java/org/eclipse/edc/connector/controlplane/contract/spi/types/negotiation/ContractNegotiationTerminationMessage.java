/*
 *  Copyright (c) 2021 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.jetbrains.annotations.Nullable;

public class ContractNegotiationTerminationMessage extends ContractRemoteMessage {

    private String rejectionReason;
    private String code;
    private Policy policy;

    @Nullable
    public String getRejectionReason() {
        return rejectionReason;
    }

    @Nullable
    public String getCode() {
        return code;
    }

    @Override
    public Policy getPolicy() {
        return policy;
    }

    public static class Builder extends ProcessRemoteMessage.Builder<ContractNegotiationTerminationMessage, Builder> {

        private Builder() {
            super(new ContractNegotiationTerminationMessage());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder rejectionReason(String rejectionReason) {
            message.rejectionReason = rejectionReason;
            return this;
        }

        public Builder code(String code) {
            message.code = code;
            return this;
        }

        public Builder policy(Policy policy) {
            message.policy = policy;
            return this;
        }

        public ContractNegotiationTerminationMessage build() {
            return super.build();
        }
    }
}
