/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.spi.credentials;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Feature;

import java.util.Map;

/**
 * Obtains and verifies credentials associated with a DID according to an implementation-specific trust model.
 */
@FunctionalInterface
@Feature(CredentialsVerifier.FEATURE)
public interface CredentialsVerifier {

    String FEATURE = "edc:identity:verifier";

    /**
     * Verifies credentials contained in the given hub.
     *
     * @param hubBaseUrl the hub base url
     * @param publicKey  the hub's public key to encrypt messages with
     */
    Result<Map<String, String>> verifyCredentials(String hubBaseUrl, PublicKeyWrapper publicKey);

}
