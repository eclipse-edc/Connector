/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.oauth2.spi;

import org.jetbrains.annotations.Nullable;

import java.security.interfaces.RSAPublicKey;

/**
 * Resolves an RSA public key.
 */
@FunctionalInterface
public interface PublicKeyResolver {

    /**
     * Resolves the key or return null if not found.
     */
    @Nullable
    RSAPublicKey resolveKey(String id);

}
