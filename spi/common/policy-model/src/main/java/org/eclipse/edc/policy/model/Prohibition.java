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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added method
 *
 */

package org.eclipse.edc.policy.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Disallows an action if its constraints are satisfied.
 */
public class Prohibition extends Rule {

    private final List<Duty> remedies = new ArrayList<>();

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitProhibition(this);
    }

    public List<Duty> getRemedies() {
        return remedies;
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

        public Builder remedy(Duty remedy) {
            rule.remedies.add(remedy);
            return this;
        }

        public Builder remedies(List<Duty> remedies) {
            rule.remedies.addAll(remedies);
            return this;
        }

        public Prohibition build() {
            return rule;
        }
    }
}
