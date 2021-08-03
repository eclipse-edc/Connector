/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A collection of permissions, prohibitions, and obligations associated with an asset. Subtypes are defined by {@link PolicyType}.
 */
public class Policy extends Identifiable {

    public interface Visitor<R> {
        R visitPolicy(Policy policy);
    }

    private List<Permission> permissions = new ArrayList<>();
    private List<Prohibition> prohibitions = new ArrayList<>();
    private List<Duty> obligations = new ArrayList<>();
    private String inheritsFrom;

    private String assigner;
    private String assignee;

    private String target;

    @JsonProperty("@type")
    private PolicyType type = PolicyType.SET;

    private Map<String, Object> extensibleProperties = new HashMap<>();

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
    public String toString() {
        return "Policy: " + uid;
    }

    private Policy() {
    }

    public static class Builder {
        private Policy policy;

        public static Builder newInstance() {
            return new Builder();
        }

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

        private Builder() {
            policy = new Policy();
        }
    }
}
