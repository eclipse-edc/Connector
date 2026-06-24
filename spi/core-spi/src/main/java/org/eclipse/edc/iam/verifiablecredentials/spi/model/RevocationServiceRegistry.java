/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.spi.result.Result;

/**
 * Registry that contains specific revocation status checker services per type.
 */
public interface RevocationServiceRegistry {
    /**
     * Register a {@link RevocationListService} per status type
     *
     * @param statusListType the {@code CredentialStatus.type} value, for example {@code "BitstringStatusListEntry"}.
     * @param service        a {@link RevocationListService} that specifically checks a particular revocation list, for example
     *                       {@code BitstringStatusListRevocationService}
     */
    void addService(String statusListType, RevocationListService service);

    /**
     * Checks the revocation status of a {@link VerifiableCredential}. If no {@link RevocationListService} was registered for a particular
     * type, the implementation must return {@link Result#success()}. If the credential does not contain any credentialStatus,
     * the result is {@link Result#success()} as well.
     *
     * @param credential The VC
     * @return {@link Result#success()} if the VC does not contain a {@link CredentialStatus}, or if all {@link CredentialStatus} objects are valid (= not revoked, not suspended), {@link Result#failure(String)} otherwise.
     */
    Result<Void> checkValidity(VerifiableCredential credential);

    /**
     * Gets the revocation status of a {@link VerifiableCredential}. If no {@link RevocationListService} was registered for a particular
     * type, the implementation must return {@link Result#success()} with a {@code null} content. If the credential does not contain any credentialStatus,
     * the result is {@link Result#success()} as well.
     *
     * @param credential The VC
     * @return {@link Result#success()} if the VC does not contain a {@link CredentialStatus}, or if all {@link CredentialStatus} objects are valid (= not revoked, not suspended), {@link Result#failure(String)} otherwise.
     */
    Result<String> getRevocationStatus(VerifiableCredential credential);
}
