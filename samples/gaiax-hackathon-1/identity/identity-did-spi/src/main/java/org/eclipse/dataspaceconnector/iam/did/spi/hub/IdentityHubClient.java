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
package org.eclipse.dataspaceconnector.iam.did.spi.hub;

import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;

import java.security.PublicKey;
import java.util.Map;

/**
 * An interface to a foreign identity hub.
 */
public interface IdentityHubClient {

    /**
     * Queries credentials from the hib.
     */
    ClientResponse<Map<String, Object>> queryCredentials(ObjectQueryRequest query, String hubUrl, PublicKey publicKey);

}
