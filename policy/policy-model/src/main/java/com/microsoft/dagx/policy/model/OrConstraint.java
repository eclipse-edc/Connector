/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

import static java.util.stream.Collectors.joining;

/**
 * A collection of child constraints where at least one must be satisfied for the constraint to be satisfied.
 */
public class OrConstraint extends MultiplicityConstraint {

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitOrConstraint(this);
    }

    @Override
    public String toString() {
        return "Or constraint: [" + constraints.stream().map(Object::toString).collect(joining(",")) + "]";
    }

    private OrConstraint() {
    }

    public static class Builder extends MultiplicityConstraint.Builder<OrConstraint, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public OrConstraint build() {
            return constraint;
        }

        private Builder() {
            constraint = new OrConstraint();
        }
    }

}
