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

import org.eclipse.edc.spi.result.Result;

/**
 * An encryption service that does nothing (no-op).
 */
public class NoopEncryptionService implements EncryptionService {
    
    @Override
    public Result<String> encrypt(String plainText) {
        return Result.success(plainText);
    }

    @Override
    public Result<String> decrypt(String cipherText) {
        return Result.success(cipherText);
    }
}
