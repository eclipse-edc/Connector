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

package org.eclipse.edc.spi.event.policydefinition;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventPayload;

import java.util.Objects;

/**
 *  Class as organizational between level to catch events of type PolicyDefinition to catch them together in an Event Subscriber
 *  Contains data related to policy definitions
 */
public abstract class PolicyDefinitionEvent<P extends PolicyDefinitionEvent.Payload> extends Event<P> {

    public static abstract class Payload extends EventPayload {
        protected String policyDefinitionId;

        public String getContractDefinitionId() {
            return policyDefinitionId;
        }
    }

    public static class Builder<E extends PolicyDefinitionEvent<P>, P extends PolicyDefinitionEvent.Payload, B extends Builder<E, P, B>> extends Event.Builder<E, P, B> {

        protected Builder(E event, P payload) {
            super(event, payload);
        }

        @SuppressWarnings("unchecked")
        public B policyDefinitionId(String policyDefinitionId) {
            event.payload.policyDefinitionId = policyDefinitionId;
            return (B) this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.policyDefinitionId);
        }

    }
}
