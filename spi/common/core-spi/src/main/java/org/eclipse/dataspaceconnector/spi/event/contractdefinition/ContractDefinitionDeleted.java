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

package org.eclipse.dataspaceconnector.spi.event.contractdefinition;

import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventPayload;

import java.util.Objects;

/**
 * Describe a ContractDefinition deletion, after this has emitted, the ContractDefinition represented by the id won't be available anymore.
 */
public class ContractDefinitionDeleted extends Event<ContractDefinitionDeleted.Payload> {

    private ContractDefinitionDeleted() {
    }

    public static class Builder extends Event.Builder<ContractDefinitionDeleted, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractDefinitionDeleted(), new Payload());
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

    public static class Payload extends EventPayload {
        private String contractDefinitionId;

        public String getContractDefinitionId() {
            return contractDefinitionId;
        }
    }
}
