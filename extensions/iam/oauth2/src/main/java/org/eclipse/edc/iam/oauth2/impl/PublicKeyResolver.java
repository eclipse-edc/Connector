/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.iam.oauth2.impl;

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
