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

package org.eclipse.edc.connector.controlplane.policy.spi.event;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Class as organizational between level to catch events of type PolicyDefinition to catch them together in an Event Subscriber
 * Contains data related to policy definitions
 */
public abstract class PolicyDefinitionEvent extends Event {

    protected String policyDefinitionId;
    protected String participantContextId;

    public String getPolicyDefinitionId() {
        return policyDefinitionId;
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    public abstract static class Builder<T extends PolicyDefinitionEvent, B extends PolicyDefinitionEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B policyDefinitionId(String policyDefinitionId) {
            event.policyDefinitionId = policyDefinitionId;
            return self();
        }

        public B participantContextId(String participantContextId) {
            event.participantContextId = participantContextId;
            return self();
        }

        public T build() {
            Objects.requireNonNull(event.policyDefinitionId);
            return event;
        }
    }

}
