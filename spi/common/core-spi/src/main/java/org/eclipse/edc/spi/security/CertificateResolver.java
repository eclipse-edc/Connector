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

package org.eclipse.edc.spi.security;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * Resolves X509 certificates.
 */
@ExtensionPoint
public interface CertificateResolver {

    /**
     * Returns the public key associated with the id or null if not found.
     */
    @Nullable
    X509Certificate resolveCertificate(String id);

}
