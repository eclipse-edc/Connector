/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.policy.model.Policy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representation an additional context for {@link IdentityService} verifiers.
 */
public class VerificationContext {

    // TODO it will be removed with the TokenGenerator/Verification refactor
    @Deprecated
    private String audience;
    private Policy policy;

    private final Map<Class<?>, Object> additional;

    private VerificationContext() {
        additional = new HashMap<>();
    }

    /**
     * Returns the audience if existent otherwise null.
     */
    public String getAudience() {
        return audience;
    }

    /**
     * Returns the {@link Policy} associated with the verification context
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Gets additional data from the context by type.
     *
     * @param type the type class.
     * @param <T>  the type of data.
     * @return the object associated with the type, or null.
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(Class<T> type) {
        return (T) additional.get(type);
    }

    public static class Builder {
        private final VerificationContext context;

        private Builder() {
            context = new VerificationContext();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder audience(String audience) {
            context.audience = audience;
            return this;
        }

        public Builder data(Class<?> clazz, Object object) {
            context.additional.put(clazz, object);
            return this;
        }

        public Builder policy(Policy policy) {
            context.policy = policy;
            return this;
        }

        public VerificationContext build() {
            Objects.requireNonNull(this.context.policy, "Policy cannot be null");
            return context;
        }
    }
}
