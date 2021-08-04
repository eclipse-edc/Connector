/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

import java.util.Objects;

public class Action {
    String type;
    String includedIn;
    Constraint constraint;

    private Action() {
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        private final Action action;

        private Builder() {
            action = new Action();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(String type) {
            action.type = type;
            return this;
        }

        public Action build() {
            Objects.requireNonNull(action.type, "type");
            return action;
        }
    }

}
