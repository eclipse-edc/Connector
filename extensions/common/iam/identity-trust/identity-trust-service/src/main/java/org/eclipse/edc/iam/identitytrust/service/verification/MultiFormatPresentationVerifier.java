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

package org.eclipse.edc.iam.identitytrust.service.verification;

import org.eclipse.edc.iam.identitytrust.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.identitytrust.spi.verification.VerifierContext;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

public class MultiFormatPresentationVerifier implements PresentationVerifier {

    private final List<CredentialVerifier> verifiers;

    public MultiFormatPresentationVerifier(CredentialVerifier... verifiers) {
        this.verifiers = List.of(verifiers);
    }

    public List<CredentialVerifier> getVerifiers() {
        return verifiers;
    }

    @Override
    public Result<Void> verifyPresentation(VerifiablePresentationContainer container, String audience) {
        var ctx = VerifierContext.Builder.newInstance()
                .audience(audience)
                .verifiers(this.verifiers)
                .build();
        return ctx.verify(container.rawVp());
    }

}
