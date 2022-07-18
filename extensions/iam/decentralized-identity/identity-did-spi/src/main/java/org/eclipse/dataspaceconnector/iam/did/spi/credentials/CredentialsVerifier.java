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

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Map;

/**
 * Obtains and verifies credentials associated with a DID according to an implementation-specific trust model.
 */
@FunctionalInterface
public interface CredentialsVerifier {


    /**
     * Verifies credentials contained in the given hub.
     *
     * @param participantDid did document of the participant.
     */
    Result<Map<String, Object>> verifyCredentials(DidDocument participantDid);
}
