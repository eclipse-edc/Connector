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

    public static class Builder extends Rule.Builder<Prohibition, Prohibition.Builder> {

        private Builder() {
            rule = new Prohibition();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder uid(String uid) {
            rule.uid = uid;
            return this;
        }

        public Prohibition build() {
            return rule;
        }
    }
}
