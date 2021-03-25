package com.microsoft.dagx.policy.model;

import static java.util.stream.Collectors.joining;

/**
 * A collection of child constraints where all must be satisfied for the constraint to be satisfied.
 */
public class AndConstraint extends MultiplicityConstraint {

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitAndConstraint(this);
    }

    @Override
    public String toString() {
        return "And constraint: [" + constraints.stream().map(Object::toString).collect(joining(",")) + "]";
    }

    private AndConstraint() {
    }

    public static class Builder extends MultiplicityConstraint.Builder<AndConstraint, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public AndConstraint build() {
            return constraint;
        }

        private Builder() {
            constraint = new AndConstraint();
        }
    }
}
