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
 *       Think-it GmbH - nested credential verification
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi.verification;


import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * The context in which a {@link CredentialVerifier} verifies its input. It delegates the verification of nested
 * documents, e.g. the credentials of a presentation, to the first verifier that can handle them.
 */
public class VerifierContext {

    private final List<CredentialVerifier> verifiers = new ArrayList<>();
    private String audience;
    private boolean credentialExpected;

    public Result<Void> verify(String rawInput) {
        return verifiers.stream().filter(cv -> cv.canHandle(rawInput))
                .findFirst()
                .map(cv -> cv.verify(rawInput, this))
                .orElse(Result.failure("No verifier could handle the input data"));
    }

    public String getAudience() {
        return audience;
    }

    /**
     * Whether the input is expected to be a credential, e.g. when verifying the credentials contained in a presentation.
     *
     * @return true if the input must be a credential
     */
    public boolean isCredentialExpected() {
        return credentialExpected;
    }

    public Builder toBuilder() {
        return new Builder()
                .verifiers(verifiers)
                .audience(audience)
                .credentialExpected(credentialExpected);
    }

    public static class Builder {
        private final VerifierContext context;

        private Builder() {
            context = new VerifierContext();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder verifier(CredentialVerifier verifier) {
            this.context.verifiers.add(verifier);
            return this;
        }

        public Builder verifiers(List<CredentialVerifier> verifiers) {
            this.context.verifiers.addAll(verifiers);
            return this;
        }

        public Builder audience(String audience) {
            this.context.audience = audience;
            return this;
        }

        public Builder credentialExpected(boolean credentialExpected) {
            this.context.credentialExpected = credentialExpected;
            return this;
        }

        public VerifierContext build() {
            return this.context;
        }
    }
}
