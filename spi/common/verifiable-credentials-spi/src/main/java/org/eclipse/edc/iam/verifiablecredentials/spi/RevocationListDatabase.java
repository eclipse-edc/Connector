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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

/**
 * Checks a {@link VerifiableCredential} for revocation. Implementors should maintain an internal cache to limit remote calls
 * to the status list credential.
 * <ul>
 *     <li>StatusList2021: the cache expiry can be configured</li>
 *     <li>BitStringStatusList: the cache expiry is dictated by the {@code credentialSubject.ttl} field</li>
 * </ul>
 * <p>
 * A credential is regarded as "valid" if its {@code statusPurpose} field matches the status list credential and if
 * the value at the index indicated by {@code statusListIndex} is "1".
 */
@FunctionalInterface
public interface RevocationListDatabase {
    Result<Void> checkValidity(VerifiableCredential credential);
}
