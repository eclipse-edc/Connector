/*
 *  Copyright (c) 2021 - 2023 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - context data
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Default context implementation.
 */
public class PolicyContextImpl implements PolicyContext {
    private final List<String> problems = new ArrayList<>();
    private final Map<Class<?>, Object> additional = new HashMap<>();

    public PolicyContextImpl() {
        this(null, emptyMap());
    }

    /**
     * Creates a new PolicyContextImpl with the given participant agent and additional context
     * data. Values in the additional map need to be of the same type defined by the key, otherwise
     * they will be omitted.
     *
     * @param agent the requesting participant agent.
     * @param additionalData additional context data.
     * @deprecated please use the default constructor
     */
    @Deprecated(since = "0.1.1")
    public PolicyContextImpl(ParticipantAgent agent, Map<Class<?>, Object> additionalData) {
        if (agent != null) {
            additional.put(ParticipantAgent.class, agent);
        }
        additionalData.forEach((key, value) -> {
            try {
                additional.put(key, value);
            } catch (ClassCastException ignore) {
                // invalid entry
            }
        });
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
    @Deprecated(since = "0.1.1")
    public ParticipantAgent getParticipantAgent() {
        return getContextData(ParticipantAgent.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getContextData(Class<T> type) {
        return (T) additional.get(type);
    }

    @Override
    public <T> void putContextData(Class<T> type, T data) {
        additional.put(type, data);
    }

    public static class Builder {

        private final PolicyContextImpl context = new PolicyContextImpl();

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder additional(Class<?> clazz, Object object) {
            context.additional.put(clazz, object);
            return this;
        }

        public PolicyContext build() {
            return context;
        }
    }
}
