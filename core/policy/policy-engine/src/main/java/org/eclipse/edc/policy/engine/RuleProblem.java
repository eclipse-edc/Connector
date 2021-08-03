/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.policy.engine;

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
    private List<ConstraintProblem> constraintProblems = new ArrayList<>();

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
        private RuleProblem ruleProblem;

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
            this.ruleProblem.constraintProblems.add(problem);
            return this;
        }

        public RuleProblem build() {
            return ruleProblem;
        }

        private Builder() {
            ruleProblem = new RuleProblem();
        }
    }

    private RuleProblem() {
    }
}
