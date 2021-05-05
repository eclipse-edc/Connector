/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of policy evaluation. If all rules are satisfied, the result will be valid.
 */
public class PolicyEvaluationResult {
    private List<RuleProblem> problems;

    public PolicyEvaluationResult() {
        problems = Collections.emptyList();
    }

    public PolicyEvaluationResult(List<RuleProblem> problems) {
        this.problems = new ArrayList<>(problems);
    }

    public boolean valid() {
        return problems.isEmpty();
    }
}
