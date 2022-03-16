/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.ids.policy;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class MockPolicyContext implements PolicyContext {
    private final ParticipantAgent agent;
    private final List<String> problems = new ArrayList<>();

    public MockPolicyContext(ParticipantAgent agent) {
        this.agent = agent;
    }

    @Override
    public void reportProblem(String problem) {
        problems.add(problem);
    }

    @Override
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    @Override
    public @NotNull List<String> getProblems() {
        return problems;
    }

    @Override
    public ParticipantAgent getParticipantAgent() {
        return agent;
    }
}
