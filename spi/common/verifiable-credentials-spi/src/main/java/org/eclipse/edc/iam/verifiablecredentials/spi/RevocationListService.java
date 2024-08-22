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

package org.eclipse.edc.iam.verifiablecredentials.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

/**
 * Checks a {@link CredentialStatus} for revocation. Implementors should maintain an internal cache to limit remote calls
 * to the status list credential.
 * <ul>
 *     <li>StatusList2021: the cache expiry can be configured</li>
 *     <li>BitStringStatusList: the cache expiry is dictated by the {@code credentialSubject.ttl} field</li>
 * </ul>
 * <p>
 * A credential is regarded as "valid" if its {@code statusPurpose} field matches the status list credential and if
 * the value at the index indicated by {@code statusListIndex} is "1".
 */
public interface RevocationListService {
    /**
     * Check the "validity" of a credential status, where validity is understood as not-revoked and not-suspended. Credentials that don't have
     * a {@code credentialStatus} object are deemed valid. If the {@code credentialStatus} object is structurally invalid, the credential is deemed invalid.
     */
    Result<Void> checkValidity(CredentialStatus credential);

    /**
     * Determines the status of a credential. If a {@code credentialStatus} object exists, the service will determine the "status purpose". It can be:
     * <ul>
     *     <li>null: {@code credentialStatus} object not present, or status purpose is present but the status credential's encoded bitstring resolves a "0" at the status index.
     *        i.e. the credential is "not revoked" and "not suspended". </li>
     *     <li>suspended: credential is temporarily deactivated, i.e. the status credential's encoded bitstring resolves a "1" at the status index</li>
     *     <li>revoked: credential is permanently deactivated, i.e. the status credential's encoded bitstring resolves a "1" at the status index</li>
     * </ul>
     *
     * @param credential The credential to inspect.
     * @return either the status purpose, if the status is active, or null, if not active or not present. returns a failure if the status check failed, or the {@code credentialStatus} object is invalid.
     */
    Result<String> getStatusPurpose(VerifiableCredential credential);
}
