package com.microsoft.dagx.policy.model;

import org.jetbrains.annotations.Nullable;

import static java.util.stream.Collectors.joining;

/**
 * An obligation that must be performed if all its constraints are satisfied.
 */
public class Duty extends Rule {

    private Permission parentPermission;

    @Nullable
    private Duty consequence;

    /**
     * If this duty is part of a permission, returns the parent permission; otherwise returns null.
     */
    @Nullable
    public Permission getParentPermission() {
        return parentPermission;
    }

    void setParentPermission(Permission permission) {
        parentPermission = permission;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitDuty(this);
    }

    @Override
    public String toString() {
        return "Duty constraint: [" + getConstraints().stream().map(Object::toString).collect(joining(",")) + "]";
    }

    public static class Builder extends Rule.Builder<Duty, Duty.Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Duty build() {
            return rule;
        }

        private Builder() {
            rule = new Duty();
        }
    }

}
