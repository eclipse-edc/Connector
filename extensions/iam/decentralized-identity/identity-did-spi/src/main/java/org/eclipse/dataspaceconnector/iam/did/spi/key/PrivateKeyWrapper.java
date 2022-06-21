/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.spi.key;

import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSSigner;

/**
 * Abstraction for handling JWE operations on different private key types such as elliptic curve and RSA keys.
 */
public interface PrivateKeyWrapper {

    /**
     * Returns the JWE encrypter for the wrapped key.
     */
    JWEDecrypter decrypter();

    /**
     * Returns the JWE verifier for the wrapped key.
     */
    JWSSigner signer();
}
