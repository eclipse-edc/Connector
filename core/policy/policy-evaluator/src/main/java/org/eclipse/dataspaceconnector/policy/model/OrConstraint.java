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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * A collection of child constraints where at least one must be satisfied for the constraint to be satisfied.
 */
@JsonDeserialize(builder = OrConstraint.Builder.class)
@JsonTypeName("dataspaceconnector:orconstraint")
public class OrConstraint extends MultiplicityConstraint {

    private OrConstraint() {
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitOrConstraint(this);
    }

    @Override
    public MultiplicityConstraint create(List<Constraint> constraints) {
        return OrConstraint.Builder.newInstance().constraints(constraints).build();
    }

    @Override
    public String toString() {
        return "Or constraint: [" + constraints.stream().map(Object::toString).collect(joining(",")) + "]";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends MultiplicityConstraint.Builder<OrConstraint, Builder> {

        private Builder() {
            constraint = new OrConstraint();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public OrConstraint build() {
            return constraint;
        }
    }

}
