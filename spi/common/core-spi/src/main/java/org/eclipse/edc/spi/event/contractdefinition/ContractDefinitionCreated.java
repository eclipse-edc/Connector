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
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.edc.spi.event.contractdefinition;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Describe a new ContractDefinition creation, after this has emitted, a ContractDefinition with a certain id will be available.
 */
public class ContractDefinitionCreated extends Event<ContractDefinitionCreated.Payload> {

    private ContractDefinitionCreated() {
    }

    public static class Builder extends Event.Builder<ContractDefinitionCreated, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractDefinitionCreated(), new Payload());
        }

        public Builder contractDefinitionId(String contractDefinitionId) {
            event.payload.contractDefinitionId = contractDefinitionId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.contractDefinitionId);
        }
    }

    /**
     * This class contains all event specific attributes of a ContractDefinition Creation Event
     *
     */
    public static class Payload extends ContractDefinitionEventPayload {
    }
}
