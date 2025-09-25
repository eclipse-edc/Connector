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

package org.eclipse.edc.connector.controlplane.transfer.spi.event;

import org.eclipse.edc.spi.event.CallbackAddresses;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class as organizational between level to catch events of type TransferProcess to catch them together in an Event Subscriber
 * Contains data related to transfer processes
 */
public abstract class TransferProcessEvent extends Event implements CallbackAddresses {

    protected String transferProcessId;
    protected List<CallbackAddress> callbackAddresses = new ArrayList<>();
    protected String assetId;
    protected String type;
    protected String contractId;
    protected String participantContextId;

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getType() {
        return type;
    }

    public String getContractId() {
        return contractId;
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    @Override
    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public abstract static class Builder<T extends TransferProcessEvent, B extends TransferProcessEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public B transferProcessId(String transferProcessId) {
            event.transferProcessId = transferProcessId;
            return (B) this;
        }

        public B assetId(String assetId) {
            event.assetId = assetId;
            return (B) this;
        }

        public B callbackAddresses(List<CallbackAddress> callbackAddresses) {
            event.callbackAddresses = callbackAddresses;
            return self();
        }

        public B type(String type) {
            event.type = type;
            return self();
        }

        public B contractId(String contractId) {
            event.contractId = contractId;
            return self();
        }

        public B participantContextId(String participantContextId) {
            event.participantContextId = participantContextId;
            return self();
        }

        public abstract B self();

        public T build() {
            Objects.requireNonNull(event.transferProcessId, "transferProcess id can't be null");
            return event;
        }

    }

}
