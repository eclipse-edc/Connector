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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides additional context for {@link IdentityService} verifiers.
 */
public class VerificationContext {

    private Set<String> scopes = new HashSet<>();

    private VerificationContext() {

    }

    /**
     * Return the scopes associated with the verification context
     *
     * @return The scope
     */
    public Set<String> getScopes() {
        return scopes;
    }

    public static class Builder {
        private final VerificationContext context;

        private Builder() {
            context = new VerificationContext();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder scopes(Collection<String> scopes) {
            context.scopes = new HashSet<>(scopes);
            return this;
        }

        public VerificationContext build() {
            return context;
        }
    }
}
