/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.demo.contract.service;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Describes a policy-based contract offered by the system.
 *
 * The {@link AssetSelectorExpression} defines which assets this contract applies to. Access control policy defines the non-public requirements for accessing a set of assets
 * governed by the contract. These requirements are therefore not advertised to the agent. For example, access control policy may require an agent to be in a business partner tier.
 * Usage policy defines the requirements governing use an agent must follow when accessing the data. This policy is advertised to agents as part of a contract.
 *
 * A participant agent may access a contract (and its associated assets) if the union of the access policy and usage policy is satisfied by the agent.
 */
public class ContractDescriptor {
    private String id;
    private AssetSelectorExpression selectorExpression;
    private Policy accessControlPolicy;
    private Policy usagePolicy;

    public AssetSelectorExpression getSelectorExpression() {
        return selectorExpression;
    }

    public Policy getAccessControlPolicy() {
        return accessControlPolicy;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public Policy getUsagePolicy() {
        return usagePolicy;
    }

    private ContractDescriptor() {
    }

    public static class Builder {
        private ContractDescriptor descriptor;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            descriptor.id = id;
            return this;
        }

        public Builder selectorExpression(AssetSelectorExpression expression) {
            descriptor.selectorExpression = expression;
            return this;
        }

        public Builder accessControlPolicy(Policy policy) {
            descriptor.accessControlPolicy = policy;
            return this;
        }

        public Builder usagePolicy(Policy policy) {
            descriptor.usagePolicy = policy;
            return this;
        }

        public ContractDescriptor build() {
            requireNonNull(descriptor.id);
            requireNonNull(descriptor.selectorExpression);
            requireNonNull(descriptor.accessControlPolicy);
            requireNonNull(descriptor.usagePolicy);
            return descriptor;
        }

        private Builder() {
            descriptor = new ContractDescriptor();
        }
    }
}
