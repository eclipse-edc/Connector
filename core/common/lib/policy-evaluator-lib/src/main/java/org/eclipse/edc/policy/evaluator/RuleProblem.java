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

package org.eclipse.edc.policy.evaluator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Rule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A problem evaluating or processing a rule.
 */
@JsonDeserialize(builder = RuleProblem.Builder.class)
public class RuleProblem {
    private Rule rule;
    private String description;
    private final List<ConstraintProblem> constraintProblems = new ArrayList<>();

    private RuleProblem() {
    }

    /**
     * Returns the rule associated with the problem.
     */
    public Rule getRule() {
        return rule;
    }

    /**
     * Returns the problem description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the constraint problems that caused the rule problem or an empty collection if the problem was not related to a specific constraint problem.
     */
    @NotNull
    public List<ConstraintProblem> getConstraintProblem() {
        return constraintProblems;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final RuleProblem ruleProblem;

        private Builder() {
            ruleProblem = new RuleProblem();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder rule(Rule rule) {
            ruleProblem.rule = rule;
            return this;
        }

        public Builder description(String description) {
            ruleProblem.description = description;
            return this;
        }

        public Builder constraintProblems(List<ConstraintProblem> problems) {
            ruleProblem.constraintProblems.addAll(problems);
            return this;
        }

        public Builder constraintProblem(ConstraintProblem problem) {
            ruleProblem.constraintProblems.add(problem);
            return this;
        }

        public RuleProblem build() {
            return ruleProblem;
        }
    }
}
