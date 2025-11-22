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

package org.eclipse.edc.encryption;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

/**
 * Provides encryption and decryption services.
 */
@ExtensionPoint
public interface EncryptionService {

    /**
     * Encrypts the given plain text.
     *
     * @param plainText the text to encrypt
     * @return the encrypted text if successful, or a failure result
     */
    Result<String> encrypt(String plainText);

    /**
     * Decrypts the given cipher text.
     *
     * @param cipherText the text to decrypt
     * @return the decrypted text if successful, or a failure result
     */
    Result<String> decrypt(String cipherText);
}
