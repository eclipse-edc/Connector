/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.api.exception.AuthenticationFailedException;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface AuthenticationService {

    /**
     * Checks whether a particular request can be authenticated.
     *
     * @param headers The headers, that contain the credential to be used, e.g. a token, Basic-Auth header, etc.
     * @throws AuthenticationFailedException when the credential passed was not acceptable (e.g. null, empty, invalid base64)
     */
    boolean isAuthenticated(Map<String, List<String>> headers);
}
