package com.microsoft.dagx.policy.model;

/**
 * An extension point that evaluates an {@link AtomicConstraint}.
 */
@FunctionalInterface
public interface AtomicConstraintFunction<RIGHT_VALUE, RULE_TYPE extends Rule, RESULT> {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     */
    RESULT evaluate(Operator operator, RIGHT_VALUE rightValue, RULE_TYPE rule);

}
