/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.core.base.policy;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default context implementation.
 */
public class PolicyContextImpl implements PolicyContext {
    private final ParticipantAgent agent;
    private final List<String> problems = new ArrayList<>();
    
    private Map<Class<?>, Object> additional = new HashMap<>();

    public PolicyContextImpl(ParticipantAgent agent) {
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
    
    @Override
    public <T> T getContextData(Class<T> type) {
        return (T) additional.get(type);
    }
    
    @Override
    public <T> void putContextData(Class<T> type, T data) {
        additional.put(type, data);
    }
}
