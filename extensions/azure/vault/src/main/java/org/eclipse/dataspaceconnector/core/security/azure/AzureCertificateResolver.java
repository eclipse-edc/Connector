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

package org.eclipse.dataspaceconnector.core.security.azure;

import org.eclipse.dataspaceconnector.common.security.PemUtil;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Resolves an X.509 certificate in Azure vault.
 */
public class AzureCertificateResolver implements CertificateResolver {
    private final Vault vault;

    public AzureCertificateResolver(Vault vault) {
        this.vault = vault;
    }

    @Override
    public X509Certificate resolveCertificate(String id) {
        try {
            String encoded = vault.resolveSecret(id);
            if (encoded == null) {
                return null;
            }
            return PemUtil.readX509Certificate(encoded);
        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }
}
