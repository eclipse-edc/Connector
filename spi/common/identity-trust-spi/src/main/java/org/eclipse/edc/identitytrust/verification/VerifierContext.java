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

package org.eclipse.edc.identitytrust.verification;


import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class VerifierContext {

    private Collection<CredentialVerifier> verifiers = new ArrayList<>();
    private String audience;

    public Collection<CredentialVerifier> getVerifiers() {
        return verifiers;
    }

    public Result<Void> verify(String rawInput) {
        return verifiers.stream().filter(cv -> cv.canHandle(rawInput))
                .findFirst()
                .map(cv -> cv.verify(rawInput, this))
                .orElse(Result.failure("No verifier could handle the input data"));
    }

    public String getAudience() {
        return audience;
    }

    public VerifierContext withAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private final VerifierContext context;

        private Builder() {
            context = new VerifierContext();
        }

        private Builder(VerifierContext verifierContext) {
            this.context = verifierContext;
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder verifier(CredentialVerifier verifier) {
            this.context.verifiers.add(verifier);
            return this;
        }

        public Builder verifiers(CredentialVerifier... verifiers) {
            this.context.verifiers = new ArrayList<>(Arrays.asList(verifiers));
            return this;
        }

        public Builder audience(String audience) {
            this.context.audience = audience;
            return this;
        }

        public VerifierContext build() {
            return this.context;
        }
    }
}
