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

package org.eclipse.edc.iam.identitytrust.core.service;

import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.verifier.PresentationVerifier;
import org.eclipse.edc.spi.result.Result;

public class MultiFormatPresentationVerifier implements PresentationVerifier {
    private final JwtPresentationVerifier jwtVerifier = new JwtPresentationVerifier();
    private final JsonLdPresentationVerifier jsonLdVerifier = new JsonLdPresentationVerifier();

    @Override
    public Result<Void> verifyPresentation(String rawVp, CredentialFormat format) {
        return switch (format) {
            case JSON_LD -> jsonLdVerifier.verifyPresentation(rawVp);
            case JWT -> jwtVerifier.verifyPresentation(rawVp);
        };
    }

}
