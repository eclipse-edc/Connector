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
 * DTO representation of a Permission.
 */
public final class PermissionDto extends Typed {
    private final String action;
    @JsonProperty("constraint")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<AtomicConstraintDto> constraints;

    public PermissionDto(String action, List<AtomicConstraintDto> constraints) {
        super("Permission");
        this.action = action;
        this.constraints = constraints;
    }

    public PermissionDto() {
        this("use", List.of());
    }

    public PermissionDto(AtomicConstraintDto constraint) {
        this("use", List.of(constraint));
    }

    public String getAction() {
        return action;
    }

    public List<AtomicConstraintDto> getConstraints() {
        return constraints;
    }

}
