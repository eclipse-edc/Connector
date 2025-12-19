/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.encryption.aes;


import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.encryption.aes.AesEncryptionExtension.NAME;


@Extension(NAME)
public class AesEncryptionExtension implements ServiceExtension {

    public static final String NAME = "AES Encryption Extension";

    @Setting(
            description = "The AES encryption key used for encrypting and decrypting data.",
            key = "edc.encryption.aes.key.alias",
            required = false
    )
    private String aesKeyAlias;

    @Inject
    private Vault vault;

    @Inject
    private Monitor monitor;

    @Inject
    private EncryptionAlgorithmRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (aesKeyAlias == null || aesKeyAlias.isBlank()) {
            monitor.info("AES encryption key alias not set; AES encryption algorithm will not be registered");
        } else {
            registry.register("aes", new AesEncryptionAlgorithm(vault, aesKeyAlias));
        }
    }

}
