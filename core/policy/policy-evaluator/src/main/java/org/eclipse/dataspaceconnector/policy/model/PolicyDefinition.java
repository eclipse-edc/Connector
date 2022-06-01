/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;
import java.util.UUID;

/**
 * A {@link PolicyDefinition} is a container for a {@link Policy} and a unique identifier. Policies by themselves do
 * not have and identity, they are value objects.
 * However, most connector runtimes will need to keep a set of policies as their reference or master data, which
 * requires them to be identifiable and addressable. In most cases this also means that they have a stable, unique
 * identity, potentially across systems. In such cases a {@link Policy} should be enveloped in a
 * {@link PolicyDefinition}.
 * <p>
 * <em>Many external Policy formats like ODRL also require policies to have an ID.</em>
 */
@JsonDeserialize(builder = PolicyDefinition.Builder.class)
public class PolicyDefinition {
    private String uid;
    private Policy policy;

    private PolicyDefinition() {
    }

    public Policy getPolicy() {
        return policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.hash(uid), policy.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicyDefinition that = (PolicyDefinition) o;
        return Objects.equals(uid, that.uid) && policy.equals(that.policy);
    }

    public String getUid() {
        return uid;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final PolicyDefinition policyDefinition;

        private Builder() {
            policyDefinition = new PolicyDefinition();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder uid(String uid) {
            policyDefinition.uid = uid;
            return this;
        }

        public Builder policy(Policy policy) {
            policyDefinition.policy = policy;
            return this;
        }

        public PolicyDefinition build() {
            if (policyDefinition.uid == null) {
                policyDefinition.uid = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(policyDefinition.policy, "Policy cannot be null!");
            return policyDefinition;
        }
    }
}
