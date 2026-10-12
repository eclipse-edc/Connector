/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.event.contractagreement;

import org.eclipse.edc.participantcontext.spi.types.ParticipantEvent;
import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Class as organizational between level to catch events of type ContractAgreement to catch them together in an Event Subscriber.
 * Contains data related to contract agreements.
 */
public abstract class ContractAgreementEvent extends Event implements ParticipantEvent {

    protected String contractAgreementId;
    protected String participantContextId;

    public String getContractAgreementId() {
        return contractAgreementId;
    }

    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    public abstract static class Builder<T extends ContractAgreementEvent, B extends ContractAgreementEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B contractAgreementId(String contractAgreementId) {
            event.contractAgreementId = contractAgreementId;
            return self();
        }

        public B participantContextId(String participantContextId) {
            event.participantContextId = participantContextId;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.contractAgreementId);

            return event;
        }
    }
}
