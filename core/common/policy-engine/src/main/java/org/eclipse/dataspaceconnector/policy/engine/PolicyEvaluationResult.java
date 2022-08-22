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

package org.eclipse.dataspaceconnector.policy.engine;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of policy evaluation. If all rules are satisfied, the result will be valid.
 */
public class PolicyEvaluationResult {
    private final List<RuleProblem> problems;

    public PolicyEvaluationResult() {
        problems = Collections.emptyList();
    }

    public PolicyEvaluationResult(List<RuleProblem> problems) {
        this.problems = new ArrayList<>(problems);
    }

    public boolean valid() {
        return problems.isEmpty();
    }

    @NotNull
    public List<RuleProblem> getProblems() {
        return problems;
    }
}
