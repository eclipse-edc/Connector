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

package org.eclipse.edc.iam.identitytrust.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * This interface gives access to the REST API of a CredentialService
 */
public interface CredentialServiceClient {
    /**
     * Sends a <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#411-query-for-presentations">presentation request</a>
     * to the specified credential service.
     * <p>
     * The CredentialService will return 401/403 error codes if either the SI Token is not authorized, if the scopes don't match or if no scopes are provided.
     * <p>
     * Note that sending a DIF Presentation Definition is not supported yet and will result in a 5xx error.
     *
     * @param credentialServiceUrl The URL of the credentialService from which the VP is to be requested
     * @param siTokenJwt           A Self-Issued ID token in JWT format, that contains the access_token
     * @param scopes               A list of strings, each containing a <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#31-access-scopes">scope definition</a>
     * @return A list of {@link VerifiablePresentationContainer} objects, or a failure if the request was unsuccessful.
     */
    Result<List<VerifiablePresentationContainer>> requestPresentation(String credentialServiceUrl, String siTokenJwt, List<String> scopes);

    /**
     * Sends a <a href="https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/HEAD/#presentation-query-message">presentation request</a>
     * to the specified credential service.
     * <p>
     * The CredentialService will return 401/403 error codes if either the SI Token is not authorized, if the scopes don't match or if no scopes are provided.
     * <p>
     * Note that sending a DIF Presentation Definition is not supported yet and will result in a 5xx error.
     *
     * @param credentialServiceUrl   The URL of the credentialService from which the VP is to be requested
     * @param siTokenJwt             A Self-Issued ID token in JWT format, that contains the access_token
     * @param presentationDefinition A {@link PresentationDefinition}
     * @return A list of {@link VerifiablePresentationContainer} objects, or a failure if the request was unsuccessful.
     */
    Result<List<VerifiablePresentationContainer>> requestPresentation(String credentialServiceUrl, String siTokenJwt, PresentationDefinition presentationDefinition);

}
