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
 *       Mercedes-Benz Tech Innovation GmbH - generalization
 *
 */

package org.eclipse.dataspaceconnector.core.defaults.certificateresolver;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class DefaultCertificateResolver implements CertificateResolver {
    public static final String HEADER = "-----BEGIN CERTIFICATE-----";
    public static final String FOOTER = "-----END CERTIFICATE-----";

    @NotNull
    private final Vault vault;

    public DefaultCertificateResolver(@NotNull Vault vault) {
        this.vault = vault;
    }

    @Override
    public @Nullable X509Certificate resolveCertificate(String id) {
        var certificateRepresentation = vault.resolveSecret(id);
        if (certificateRepresentation == null) {
            return null;
        }

        var encoded = certificateRepresentation.replace(HEADER, "").replaceAll(System.lineSeparator(), "").replace(FOOTER, "");

        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            return (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(encoded.getBytes())));
        } catch (GeneralSecurityException e) {
            throw new EdcException(String.format("Found certificate with id [%s], but failed to convert it", id), e);
        }
    }
}
