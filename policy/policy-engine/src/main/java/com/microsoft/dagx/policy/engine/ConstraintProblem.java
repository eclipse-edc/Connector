package com.microsoft.dagx.policy.engine;

import com.microsoft.dagx.policy.model.Constraint;

/**
 * A problem encountered during evaluation or processing of a constraint such as an unsatisfied constraint.
 */
public class ConstraintProblem {
    private String description;
    private Constraint constraint;

    public ConstraintProblem(String description, Constraint constraint) {
        this.description = description;
        this.constraint = constraint;
    }

    /**
     * Returns a problem description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the constraint associated with the problem.
     */
    public Constraint getConstraint() {
        return constraint;
    }
}
