/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.spi.event.policydefinition;

/**
 * Describe a PolicyDefinition update, after this has emitted, a PolicyDefinition with a certain id will be available/updated.
 */
public class PolicyDefinitionUpdated extends PolicyDefinitionEvent<PolicyDefinitionUpdated.Payload> {

    private PolicyDefinitionUpdated() {
    }

    /**
     * This class contains all event specific attributes of a PolicyDefinition Update Event
     *
     */
    public static class Payload extends PolicyDefinitionEvent.Payload {
    }

    public static class Builder extends PolicyDefinitionEvent.Builder<PolicyDefinitionUpdated, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new PolicyDefinitionUpdated(), new Payload());
        }
    }

}
