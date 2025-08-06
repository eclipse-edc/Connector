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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added method
 *
 */

package org.eclipse.edc.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Allows an action if its constraints are satisfied.
 */
@JsonDeserialize(builder = Permission.Builder.class)
@JsonTypeName("dataspaceconnector:permission")
public class Permission extends Rule {
    private final List<Duty> duties = new ArrayList<>();

    public List<Duty> getDuties() {
        return duties;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitPermission(this);
    }

    @Override
    public String toString() {
        return "Permission constraints: [" + getConstraints().stream().map(Object::toString).collect(joining(",")) + "]";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Rule.Builder<Permission, Builder> {

        private Builder() {
            rule = new Permission();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder duty(Duty duty) {
            rule.duties.add(duty);
            return this;
        }

        public Builder duties(List<Duty> duties) {
            rule.duties.addAll(duties);
            return this;
        }

        public Permission build() {
            return rule;
        }
    }
}
