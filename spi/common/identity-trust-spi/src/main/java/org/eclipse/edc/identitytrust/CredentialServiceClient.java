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

package org.eclipse.edc.identitytrust;

import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * This interface gives access to the REST API of a CredentialService
 */
public interface CredentialServiceClient {
    /**
     * Sends a <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#411-query-for-presentations">presentation request</a>
     * to the specified credential service.
     *
     * @param csUrl      The URL of the CredentialService, from which the presentation is to be requested.
     * @param siTokenJwt A Self-Issued ID token in JWT format, that contains the access_token
     * @param scopes     A list of strings, each containing a <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#31-access-scopes">scope definition</a>
     */
    Result<VerifiablePresentationContainer> requestPresentation(String csUrl, String siTokenJwt, List<String> scopes);

    //todo: add write api?
}
