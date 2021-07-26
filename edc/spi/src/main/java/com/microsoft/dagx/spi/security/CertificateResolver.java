/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.security;

import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * Resolves X509 certificates.
 */
public interface CertificateResolver {

    /**
     * Returns the public key associated with the id or null if not found.
     */
    @Nullable
    X509Certificate resolveCertificate(String id);

}
