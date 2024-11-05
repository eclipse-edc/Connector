/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.policy.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

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
public class PolicyDefinition extends Entity {
    public static final String EDC_POLICY_DEFINITION_TYPE = EDC_NAMESPACE + "PolicyDefinition";
    public static final String EDC_POLICY_DEFINITION_POLICY = EDC_NAMESPACE + "policy";
    public static final String EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES = EDC_NAMESPACE + "privateProperties";
    private Policy policy;
    private final Map<String, Object> privateProperties = new HashMap<>();

    private PolicyDefinition() {
    }

    public Policy getPolicy() {
        return policy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Objects.hash(id), policy.hashCode(), privateProperties);
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
        return Objects.equals(id, that.id) && policy.equals(that.policy);
    }

    @NotNull
    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

    public Object getPrivateProperty(String key) {
        return privateProperties.get(key);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends Entity.Builder<PolicyDefinition, Builder> {

        private Builder() {
            super(new PolicyDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder policy(Policy policy) {
            entity.policy = policy;
            return this;
        }

        public Builder privateProperty(String key, Object value) {
            entity.privateProperties.put(key, value);
            return this;
        }

        public Builder privateProperties(Map<String, Object> privateProperties) {
            Objects.requireNonNull(privateProperties);
            entity.privateProperties.putAll(privateProperties);
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public PolicyDefinition build() {
            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(entity.policy, "Policy cannot be null!");
            return super.build();
        }
    }
}
