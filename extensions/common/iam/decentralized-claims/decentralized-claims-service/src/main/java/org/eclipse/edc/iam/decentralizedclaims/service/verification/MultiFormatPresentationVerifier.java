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

package org.eclipse.edc.iam.decentralizedclaims.service.verification;

import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifierRegistry;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.VerifierContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.function.Supplier;

public class MultiFormatPresentationVerifier implements PresentationVerifier {

    private final Supplier<List<CredentialVerifier>> verifiers;

    /**
     * Verifies presentations with the verifiers registered in the {@link CredentialVerifierRegistry} at the time of
     * the verification, so that verifiers registered by other extensions are included.
     *
     * @param registry the registry of the verifiers
     */
    public MultiFormatPresentationVerifier(CredentialVerifierRegistry registry) {
        this.verifiers = registry::getVerifiers;
    }

    public MultiFormatPresentationVerifier(CredentialVerifier... verifiers) {
        var list = List.of(verifiers);
        this.verifiers = () -> list;
    }

    public List<CredentialVerifier> getVerifiers() {
        return verifiers.get();
    }

    @Override
    public Result<Void> verifyPresentation(VerifiablePresentationContainer container, String audience) {
        var ctx = VerifierContext.Builder.newInstance()
                .audience(audience)
                .verifiers(verifiers.get())
                .presentationExpected(true)
                .build();
        return ctx.verify(container.rawVp());
    }

}
