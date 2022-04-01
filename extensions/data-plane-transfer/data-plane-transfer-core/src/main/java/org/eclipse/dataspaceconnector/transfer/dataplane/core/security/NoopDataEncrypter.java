/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.core.security;

import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

/**
 * Implementation of {@link DataEncrypter} which does not perform any encryption.
 */
public class NoopDataEncrypter implements DataEncrypter {
    @Override
    public String encrypt(String raw) {
        return raw;
    }

    @Override
    public String decrypt(String encrypted) {
        return encrypted;
    }
}
