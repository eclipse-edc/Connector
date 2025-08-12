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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - expending Event classes
 *
 */

package org.eclipse.edc.connector.controlplane.policy.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Describe a PolicyDefinition deletion, after this has emitted, the PolicyDefinition represented by the id won't be available anymore.
 */
@JsonDeserialize(builder = PolicyDefinitionDeleted.Builder.class)
public class PolicyDefinitionDeleted extends PolicyDefinitionEvent {

    private PolicyDefinitionDeleted() {
    }

    @Override
    public String name() {
        return "policy.definition.deleted";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends PolicyDefinitionEvent.Builder<PolicyDefinitionDeleted, Builder> {

        private Builder() {
            super(new PolicyDefinitionDeleted());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
