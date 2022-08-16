/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system.vault;

import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

public class NoopCertificateResolver implements CertificateResolver {
    @Override
    public @Nullable X509Certificate resolveCertificate(String key) {
        return null;
    }
}
