/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Resolves an X.509 certificate in Hashicorp vault.
 */
public class HashicorpCertificateResolver implements CertificateResolver {
    @NotNull
    private final Vault vault;
    @NotNull
    private final Monitor monitor;

    public HashicorpCertificateResolver(@NotNull Vault vault, @NotNull Monitor monitor) {
        this.vault = vault;
        this.monitor = monitor;
    }

    @Override
    public X509Certificate resolveCertificate(String id) {
        var certificateRepresentation = vault.resolveSecret(id);
        if (certificateRepresentation == null) {
            return null;
        }
        try (InputStream inputStream =
                     new ByteArrayInputStream(certificateRepresentation.getBytes(StandardCharsets.UTF_8))) {
            var x509Certificate = PemUtil.readX509Certificate(inputStream);
            if (x509Certificate == null) {
                monitor.warning(
                        String.format("Expected PEM certificate on key %s, but value not PEM.", id));
            }
            return x509Certificate;
        } catch (IOException | CertificateException e) {
            throw new EdcException(e.getMessage(), e);
        }
    }
}
