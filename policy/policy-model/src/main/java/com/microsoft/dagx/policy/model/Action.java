/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

import java.util.Objects;

public class Action {
    String type;
    String includedIn;
    Constraint constraint;

    public String getType() {
        return type;
    }

    private Action() {
    }

    public static class Builder {
        private Action action;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(String type) {
            this.action.type = type;
            return this;
        }

        public Action build() {
            Objects.requireNonNull(action.type, "type");
            return action;
        }

        private Builder() {
            action = new Action();
        }
    }

}
