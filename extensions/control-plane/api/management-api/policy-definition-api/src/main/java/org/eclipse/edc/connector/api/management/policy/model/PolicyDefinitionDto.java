/*
 *  Copyright (c) 2023 T-Systems International GmbH
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

package org.eclipse.edc.connector.api.management.policy.model;

import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.policy.model.Policy;

public abstract class PolicyDefinitionDto {

    protected Policy policy;

    protected abstract static class Builder<A extends PolicyDefinitionDto, B extends Builder<A, B>> {

        protected final A dto;

        protected Builder(A dto) {
            this.dto = dto;
        }

        public B policy(Policy policy) {
            dto.policy = policy;
            return self();
        }

        public abstract B self();

        public A build() {
            return dto;
        }
    }
}