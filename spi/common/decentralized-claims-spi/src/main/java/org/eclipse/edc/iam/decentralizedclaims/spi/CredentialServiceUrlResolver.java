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

package org.eclipse.edc.iam.decentralizedclaims.spi;

import org.eclipse.edc.spi.result.Result;

/**
 * This is a functional interface used to resolve the URL of the credential service based on the issuer. Typically, this
 * is done based on the issuer's DID document
 */
@FunctionalInterface
public interface CredentialServiceUrlResolver {
    /**
     * Resolves the URL of the credential service based on the issuer.
     *
     * @param issuer the issuer for which the URL needs to be resolved
     * @return a {@link Result} object containing the resolved URL if successful, or the failure information if unsuccessful
     */
    Result<String> resolve(String issuer);
}
