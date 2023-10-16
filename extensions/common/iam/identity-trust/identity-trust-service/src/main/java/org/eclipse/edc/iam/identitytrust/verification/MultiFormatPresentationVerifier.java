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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.identitytrust.verification.JwtVerifier;
import org.eclipse.edc.identitytrust.verification.PresentationVerifier;
import org.eclipse.edc.spi.result.Result;

public class MultiFormatPresentationVerifier implements PresentationVerifier {
    private final JwtPresentationVerifier jwtPresentationVerifier;
    private final JsonLdPresentationVerifier jsonLdVerifier;

    public MultiFormatPresentationVerifier(JwtVerifier tokenVerifier, String audience, ObjectMapper mapper) {
        jwtPresentationVerifier = new JwtPresentationVerifier(tokenVerifier, audience, mapper);
        jsonLdVerifier = new JsonLdPresentationVerifier();
    }

    @Override
    public Result<Void> verifyPresentation(VerifiablePresentationContainer container) {
        var vpResult = switch (container.format()) {
            case JSON_LD -> jsonLdVerifier.verifyPresentation(container.rawVp());
            case JWT -> jwtPresentationVerifier.verifyPresentation(container.rawVp());
        };


        return vpResult;
    }

}
