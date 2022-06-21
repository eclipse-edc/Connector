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

package org.eclipse.dataspaceconnector.spi.types.domain.contract.offer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Defines the parameters of a contract.
 * <p>
 * The {@link AssetSelectorExpression} defines which assets this contract applies to. Access policy defines the non-public requirements for accessing a set of assets
 * governed by the contract. These requirements are therefore not advertised to the agent. For example, access control policy may require an agent to be in a business partner tier.
 * Contract policy defines the requirements governing use an agent must follow when accessing the data. This policy is advertised to agents as part of a contract.
 * <p>
 * A participant agent may access a contract (and its associated assets) if the union of the access policy and contract policy is satisfied by the agent.
 * <p>
 * Note that the id must be a UUID.
 */
@JsonDeserialize(builder = ContractDefinition.Builder.class)
public class ContractDefinition {
    private String id;
    private String accessPolicyId;
    private String contractPolicyId;
    private AssetSelectorExpression selectorExpression;

    private ContractDefinition() {
    }

    public String getId() {
        return id;
    }

    @NotNull
    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    @NotNull
    public String getContractPolicyId() {
        return contractPolicyId;
    }

    @NotNull
    public AssetSelectorExpression getSelectorExpression() {
        return selectorExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractDefinition that = (ContractDefinition) o;
        return Objects.equals(id, that.id) && Objects.equals(accessPolicyId, that.accessPolicyId) && Objects.equals(contractPolicyId, that.contractPolicyId) && Objects.equals(selectorExpression, that.selectorExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accessPolicyId, contractPolicyId, selectorExpression);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final ContractDefinition definition;

        private Builder() {
            definition = new ContractDefinition();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            definition.id = id;
            return this;
        }

        public Builder accessPolicyId(String policyId) {
            definition.accessPolicyId = policyId;
            return this;
        }

        public Builder contractPolicyId(String policyId) {
            definition.contractPolicyId = policyId;
            return this;
        }

        public Builder selectorExpression(AssetSelectorExpression expression) {
            definition.selectorExpression = expression;
            return this;
        }

        public ContractDefinition build() {
            Objects.requireNonNull(definition.id);
            Objects.requireNonNull(definition.accessPolicyId);
            Objects.requireNonNull(definition.contractPolicyId);
            Objects.requireNonNull(definition.selectorExpression);
            return definition;
        }
    }
}
