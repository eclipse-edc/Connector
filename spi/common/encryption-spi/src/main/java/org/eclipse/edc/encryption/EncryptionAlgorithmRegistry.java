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
 * Registry for encryption algorithms.
 */
@ExtensionPoint
public interface EncryptionAlgorithmRegistry {
    
    /**
     * Registers an encryption algorithm with the given name.
     *
     * @param algorithm the name of the algorithm
     * @param service   the encryption algorithm service
     */
    void register(String algorithm, EncryptionAlgorithm service);

    /**
     * Checks if the registry supports the given algorithm.
     *
     * @param algorithm the algorithm to check
     * @return true if supported, false otherwise
     */
    boolean supports(String algorithm);

    /**
     * Encrypts the given plain text.
     *
     * @param algorithm the algorithm to use for encryption
     * @param plainText the text to encrypt
     * @return the encrypted text if successful, or a failure result
     */
    Result<String> encrypt(String algorithm, String plainText);

    /**
     * Decrypts the given cipher text.
     *
     * @param algorithm  the algorithm to use for decryption
     * @param cipherText the text to decrypt
     * @return the decrypted text if successful, or a failure result
     */
    Result<String> decrypt(String algorithm, String cipherText);
}
