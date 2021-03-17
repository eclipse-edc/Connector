package com.microsoft.dagx.policy.engine.model;

import org.jetbrains.annotations.Nullable;

import static java.util.stream.Collectors.joining;

/**
 * Allows an action if its constraints are satisfied.
 */
public class Permission extends Rule {
    @Nullable
    private Duty duty;

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

        public Permission build() {
            return rule;
        }

        private Builder() {
            rule = new Permission();
        }
    }
}
