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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default context implementation.
 */
public abstract class PolicyContextImpl implements PolicyContext {
    private final List<String> problems = new ArrayList<>();
    private final Map<Class<?>, Object> additional = new HashMap<>();

    protected PolicyContextImpl() {
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
    @SuppressWarnings("unchecked")
    public <T> T getContextData(Class<T> type) {
        return (T) additional.get(type);
    }

    @Override
    public <T> void putContextData(Class<T> type, T data) {
        additional.put(type, data);
    }

    @Deprecated(since = "0.10.0")
    public static class Builder {

        private final PolicyContextImpl context = new PolicyContextImpl() {

            @Override
            public String scope() {
                return "";
            }
        };

        private Builder() {

        }

        @Deprecated(since = "0.10.0")
        public static Builder newInstance() {
            return new Builder();
        }

        @Deprecated(since = "0.10.0")
        public Builder additional(Class<?> clazz, Object object) {
            context.additional.put(clazz, object);
            return this;
        }

        @Deprecated(since = "0.10.0")
        public PolicyContext build() {
            return context;
        }
    }
}
