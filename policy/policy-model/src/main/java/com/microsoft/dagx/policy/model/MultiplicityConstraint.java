/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of child constraints. Subclasses define the semantics for when this constraint is satisfied.
 */
public abstract class MultiplicityConstraint extends Constraint {
    protected List<Constraint> constraints = new ArrayList<>();

    public List<Constraint> getConstraints() {
        return constraints;
    }

    protected abstract static class Builder<T extends MultiplicityConstraint, B extends Builder<T, B>> {
        protected T constraint;

        @SuppressWarnings("unchecked")
        public B constraint(Constraint constraint) {
            this.constraint.constraints.add(constraint);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B constraints(List<Constraint> constraints) {
            constraint.constraints.addAll(constraints);
            return (B) this;
        }

    }

}
