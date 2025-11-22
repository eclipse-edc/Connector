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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;

/**
 * A remote message related to the TransferProcess context
 */
public abstract class TransferRemoteMessage extends ProcessRemoteMessage {

    protected Policy policy;

    @Override
    public Policy getPolicy() {
        return policy;
    }

    public abstract static class Builder<M extends TransferRemoteMessage, B extends Builder<M, B>> extends ProcessRemoteMessage.Builder<M, B> {

        protected Builder(M message) {
            super(message);
        }

        public B policy(Policy policy) {
            message.policy = policy;
            return self();
        }
    }

}
