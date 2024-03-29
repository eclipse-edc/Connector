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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.dataplane.spi.security;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Interface for encryption/decryption of sensible data.
 * This is especially used to secure the data address encoded as claim in the security token.
 */
@ExtensionPoint
public interface DataEncrypter {
    String encrypt(String raw);

    String decrypt(String encrypted);
}
