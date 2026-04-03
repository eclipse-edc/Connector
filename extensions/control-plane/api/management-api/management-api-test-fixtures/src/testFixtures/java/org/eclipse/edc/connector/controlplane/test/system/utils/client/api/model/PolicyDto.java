/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representation of a Policy.
 */
public class PolicyDto extends Typed {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String assigner;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String target;
    @JsonProperty("permission")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<PermissionDto> permissions;

    public PolicyDto(String type,
                     String assigner,
                     String target,
                     List<PermissionDto> permissions) {
        super(type);
        this.assigner = assigner;
        this.target = target;
        this.permissions = permissions;
    }

    public PolicyDto() {
        this("Set", null, null, List.of());
    }

    public PolicyDto(String type) {
        this(type, null, null, List.of());
    }

    public PolicyDto(List<PermissionDto> permissions) {
        this("Set", null, null, permissions);
    }

    public String getAssigner() {
        return assigner;
    }

    public String getTarget() {
        return target;
    }

    public List<PermissionDto> getPermissions() {
        return permissions;
    }

}
