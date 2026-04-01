/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.virtual.controlplane.transfer.subscriber.nats;

import org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.nats.tasks.subscriber.AbstractTaskSubscriber;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.Objects;

public class NatsTransferProcessTaskSubscriber extends AbstractTaskSubscriber<TransferProcessTaskPayload> {

    private TransferProcessTaskExecutor taskExecutor;

    private NatsTransferProcessTaskSubscriber() {
        super(TransferProcessTaskPayload.class);
    }

    @Override
    protected StatusResult<Void> handlePayload(TransferProcessTaskPayload payload) {
        return taskExecutor.handle(payload);
    }

    public static class Builder extends AbstractTaskSubscriber.Builder<NatsTransferProcessTaskSubscriber, Builder, TransferProcessTaskPayload> {

        protected Builder() {
            super(new NatsTransferProcessTaskSubscriber());
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public Builder taskExecutor(TransferProcessTaskExecutor taskManager) {
            subscriber.taskExecutor = taskManager;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public NatsTransferProcessTaskSubscriber build() {
            Objects.requireNonNull(subscriber.mapperSupplier, "mapperSupplier must be set");
            Objects.requireNonNull(subscriber.taskExecutor, "stateMachineService must be set");
            return super.build();
        }
    }
}
