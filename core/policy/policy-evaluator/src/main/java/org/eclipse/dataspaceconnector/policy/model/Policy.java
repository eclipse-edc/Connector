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
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A collection of permissions, prohibitions, and obligations associated with an asset. Subtypes are defined by {@link PolicyType}.
 */
@JsonDeserialize(builder = Policy.Builder.class)
public class Policy extends Identifiable {

    private final List<Permission> permissions = new ArrayList<>();
    private final List<Prohibition> prohibitions = new ArrayList<>();
    private final List<Duty> obligations = new ArrayList<>();
    private final Map<String, Object> extensibleProperties = new HashMap<>();
    private String inheritsFrom;
    private String assigner;
    private String assignee;
    private String target;
    @JsonProperty("@type")
    private PolicyType type = PolicyType.SET;

    private Policy() {
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public List<Prohibition> getProhibitions() {
        return prohibitions;
    }

    public List<Duty> getObligations() {
        return obligations;
    }

    @Nullable
    public String getInheritsFrom() {
        return inheritsFrom;
    }

    public String getAssigner() {
        return assigner;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getTarget() {
        return target;
    }

    public PolicyType getType() {
        return type;
    }

    public Map<String, Object> getExtensibleProperties() {
        return extensibleProperties;
    }

    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitPolicy(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissions, prohibitions, obligations, extensibleProperties, inheritsFrom, assigner, assignee, target, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Policy policy = (Policy) o;
        return permissions.equals(policy.permissions) && prohibitions.equals(policy.prohibitions) && obligations.equals(policy.obligations) && extensibleProperties.equals(policy.extensibleProperties) &&
                Objects.equals(inheritsFrom, policy.inheritsFrom) && Objects.equals(assigner, policy.assigner) && Objects.equals(assignee, policy.assignee) && Objects.equals(target, policy.target) && type == policy.type;
    }

    @Override
    public String toString() {
        return "Policy: " + uid;
    }

    public interface Visitor<R> {
        R visitPolicy(Policy policy);
    }
    
    /**
     * Returns a copy of this policy with the specified target.
     *
     * @param target the target.
     * @return a copy with the specified target.
     */
    public Policy withTarget(String target) {
        return Builder.newInstance()
                .id(this.uid)
                .prohibitions(this.prohibitions.stream().map(p -> p.withTarget(target)).collect(Collectors.toList()))
                .permissions(this.permissions.stream().map(p -> p.withTarget(target)).collect(Collectors.toList()))
                .duties(this.obligations.stream().map(o -> o.withTarget(target)).collect(Collectors.toList()))
                .assigner(this.assigner)
                .assignee(this.assignee)
                .inheritsFrom(this.inheritsFrom)
                .type(this.type)
                .extensibleProperties(this.extensibleProperties)
                .target(target)
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final Policy policy;

        private Builder() {
            policy = new Policy();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("uid")
        public Builder id(String id) {
            policy.uid = id;
            return this;
        }

        public Builder prohibition(Prohibition prohibition) {
            policy.prohibitions.add(prohibition);
            return this;
        }

        public Builder prohibitions(List<Prohibition> prohibitions) {
            policy.prohibitions.addAll(prohibitions);
            return this;
        }

        public Builder permission(Permission permission) {
            policy.permissions.add(permission);
            return this;
        }

        public Builder permissions(List<Permission> permissions) {
            policy.permissions.addAll(permissions);
            return this;
        }

        public Builder duty(Duty duty) {
            policy.obligations.add(duty);
            return this;
        }

        @JsonProperty("obligations")
        public Builder duties(List<Duty> duties) {
            policy.obligations.addAll(duties);
            return this;
        }

        public Builder duty(String inheritsFrom) {
            policy.inheritsFrom = inheritsFrom;
            return this;
        }

        public Builder assigner(String assigner) {
            policy.assigner = assigner;
            return this;
        }

        public Builder assignee(String assignee) {
            policy.assignee = assignee;
            return this;
        }

        public Builder target(String target) {
            policy.target = target;
            return this;
        }

        public Builder inheritsFrom(String inheritsFrom) {
            policy.inheritsFrom = inheritsFrom;
            return this;
        }

        @JsonProperty("@type")
        public Builder type(PolicyType type) {
            policy.type = type;
            return this;
        }

        public Builder extensibleProperty(String key, Object value) {
            policy.extensibleProperties.put(key, value);
            return this;
        }

        public Builder extensibleProperties(Map<String, Object> properties) {
            policy.extensibleProperties.putAll(properties);
            return this;
        }

        public Policy build() {
            if (policy.uid == null) {
                policy.uid = UUID.randomUUID().toString();
            }
            return policy;
        }
    }
}
