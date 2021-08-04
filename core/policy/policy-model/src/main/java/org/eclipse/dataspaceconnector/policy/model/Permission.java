/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

import org.jetbrains.annotations.Nullable;

import static java.util.stream.Collectors.joining;

/**
 * Allows an action if its constraints are satisfied.
 */
public class Permission extends Rule {
    private Duty duty;

    @Nullable
    public Duty getDuty() {
        return duty;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitPermission(this);
    }

    @Override
    public String toString() {
        return "Permission constraints: [" + getConstraints().stream().map(Object::toString).collect(joining(",")) + "]";
    }

    public static class Builder extends Rule.Builder<Permission, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder duty(Duty duty) {
            this.rule.duty = duty;
            this.rule.duty.setParentPermission(this.rule);
            return this;
        }

        public Permission build() {
            return rule;
        }

        private Builder() {
            rule = new Permission();
        }
    }
}
