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

package org.eclipse.edc.iam.identitytrust.verification;

import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.identitytrust.verification.CredentialVerifier;
import org.eclipse.edc.identitytrust.verification.PresentationVerifier;
import org.eclipse.edc.identitytrust.verification.VerifierContext;
import org.eclipse.edc.spi.result.Result;

public class MultiFormatPresentationVerifier implements PresentationVerifier {

    private final VerifierContext context;

    public MultiFormatPresentationVerifier(String audience, CredentialVerifier... verifiers) {

        this.context = VerifierContext.Builder.newInstance()
                .verifiers(verifiers)
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
