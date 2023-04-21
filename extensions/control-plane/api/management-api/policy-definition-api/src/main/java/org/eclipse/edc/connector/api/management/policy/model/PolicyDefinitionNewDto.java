/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

import jakarta.json.JsonObject;
import jakarta.validation.constraints.NotNull;


public abstract class PolicyDefinitionNewDto {

    @NotNull
    protected JsonObject policy;

    public JsonObject getPolicy() {
        return policy;
    }

    protected abstract static class Builder<A extends PolicyDefinitionNewDto, B extends Builder<A, B>> {

        protected final A dto;

        protected Builder(A dto) {
            this.dto = dto;
        }

        public B policy(JsonObject policy) {
            dto.policy = policy;
            return self();
        }

        public abstract B self();

        public A build() {
            return dto;
        }
    }
}
