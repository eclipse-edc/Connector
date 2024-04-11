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

    private final VerifierContext context;

    public MultiFormatPresentationVerifier(String audience, CredentialVerifier... verifiers) {

        this.context = VerifierContext.Builder.newInstance()
                .verifiers(List.of(verifiers))
                .audience(audience).build();
    }

    public VerifierContext getContext() {
        return context;
    }

    @Override
    public Result<Void> verifyPresentation(VerifiablePresentationContainer container) {
        return context.verify(container.rawVp());
    }

}
