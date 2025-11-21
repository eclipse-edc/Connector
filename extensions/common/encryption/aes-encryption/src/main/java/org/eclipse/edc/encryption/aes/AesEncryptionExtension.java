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


import org.eclipse.edc.encryption.EncryptionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.encryption.aes.AesEncryptionExtension.NAME;


@Extension(NAME)
public class AesEncryptionExtension implements ServiceExtension {

    public static final String NAME = "AES Encryption Extension";

    @Setting(
            description = "The AES encryption key used for encrypting and decrypting data.",
            key = "edc.encryption.aes.key.alias"
    )
    private String aesKeyAlias;

    @Inject
    private Vault vault;

    @Provider
    public EncryptionService encryptionService() {
        return new AesEncryptionService(vault, aesKeyAlias);
    }
}
