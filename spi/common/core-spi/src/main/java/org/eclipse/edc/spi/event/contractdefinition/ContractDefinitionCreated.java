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

/**
 * Describe a new ContractDefinition creation, after this has emitted, a ContractDefinition with a certain id will be available.
 */
public class ContractDefinitionCreated extends ContractDefinitionEvent<ContractDefinitionCreated.Payload> {

    private ContractDefinitionCreated() {
    }

    /**
     * This class contains all event specific attributes of a ContractDefinition Creation Event
     *
     */
    public static class Payload extends ContractDefinitionEvent.Payload {
    }

    public static class Builder extends ContractDefinitionEvent.Builder<ContractDefinitionCreated, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractDefinitionCreated(), new Payload());
        }
    }

}
