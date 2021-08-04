/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

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
