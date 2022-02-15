/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonDeserialize(builder = PolicyDefinitionDto.Builder.class)
public class PolicyDefinitionDto {
    private String uid;
    private List<Permission> permissions = new ArrayList<>();
    private List<Prohibition> prohibitions = new ArrayList<>();
    private List<Duty> obligations = new ArrayList<>();
    private Map<String, Object> extensibleProperties = new HashMap<>();
    private String inheritsFrom;
    private PolicyType type = PolicyType.SET;

    private PolicyDefinitionDto() {
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

    public Map<String, Object> getExtensibleProperties() {
        return extensibleProperties;
    }

    public String getInheritsFrom() {
        return inheritsFrom;
    }

    @JsonProperty("@type")
    public PolicyType getType() {
        return type;
    }


    public String getUid() {
        return uid;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final PolicyDefinitionDto dto;

        private Builder() {
            dto = new PolicyDefinitionDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("uid")
        public Builder id(String id) {
            dto.uid = id;
            return this;
        }

        public Builder permissions(List<Permission> permissions) {
            dto.permissions = permissions;
            return this;
        }

        public Builder prohibitions(List<Prohibition> prohibitions) {
            dto.prohibitions = prohibitions;
            return this;
        }

        public Builder obligations(List<Duty> obligations) {
            dto.obligations = obligations;
            return this;
        }

        public Builder extensibleProperties(Map<String, Object> extensibleProperties) {
            dto.extensibleProperties = extensibleProperties;
            return this;
        }

        public Builder inheritsFrom(String inheritsFrom) {
            dto.inheritsFrom = inheritsFrom;
            return this;
        }

        @JsonProperty("@type")
        public Builder type(PolicyType type) {
            dto.type = type;
            return this;
        }

        public PolicyDefinitionDto build() {
            return dto;
        }
    }
}
