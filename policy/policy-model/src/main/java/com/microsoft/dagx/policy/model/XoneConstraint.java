package com.microsoft.dagx.policy.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * A collection of child constraints where exactly one must be satisfied for the constraint to be satisfied.
 */
public class XoneConstraint extends MultiplicityConstraint {
    private List<Constraint> constraints = new ArrayList<>();

    public List<Constraint> getConstraints() {
        return constraints;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitXoneConstraint(this);
    }

    @Override
    public String toString() {
        return "Xone constraint: [" + constraints.stream().map(Object::toString).collect(joining(",")) + "]";
    }

    public static class Builder extends MultiplicityConstraint.Builder<XoneConstraint, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public XoneConstraint build() {
            return constraint;
        }

        private Builder() {
            constraint = new XoneConstraint();
        }
    }

}
