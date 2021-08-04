/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.policy.model;

import static java.util.stream.Collectors.joining;

/**
 * Disallows an action if its constraints are satisfied.
 */
public class Prohibition extends Rule {

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitProhibition(this);
    }

    @Override
    public String toString() {
        return "Prohibition constraints: [" + getConstraints().stream().map(Object::toString).collect(joining(",")) + "]";
    }

    public static class Builder extends Rule.Builder<Prohibition, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Prohibition build(){
            return rule;
        }

        private Builder() {
            rule = new Prohibition();
        }
    }
}
