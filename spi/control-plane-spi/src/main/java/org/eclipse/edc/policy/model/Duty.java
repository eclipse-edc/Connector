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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * An obligation that must be performed if all its constraints are satisfied.
 */
@JsonDeserialize(builder = Duty.Builder.class)
@JsonTypeName("dataspaceconnector:duty")
public class Duty extends Rule {

    private final List<Duty> consequences = new ArrayList<>();

    public List<Duty> getConsequences() {
        return consequences;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitDuty(this);
    }

    @Override
    public String toString() {
        return "Duty constraint: [" + getConstraints().stream().map(Object::toString).collect(joining(",")) + "]";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Rule.Builder<Duty, Duty.Builder> {

        private Builder() {
            rule = new Duty();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder consequence(Duty consequence) {
            rule.consequences.add(consequence);
            return this;
        }

        public Builder consequences(List<Duty> consequences) {
            rule.consequences.addAll(consequences);
            return this;
        }

        public Duty build() {
            return rule;
        }
    }

}
