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

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSVerifier;

/**
 * Abstraction for handling JWE operations on different public key types such as elliptic curve and RSA keys.
 */
public interface PublicKeyWrapper {

    /**
     * Returns the JWE encrypter for the wrapped key.
     */
    JWEEncrypter encrypter();

    /**
     * Returns the JWE verifier for the wrapped key.
     */
    JWSVerifier verifier();

    /**
     * Returns the wrapped key algorithm.
     */
    default JWEAlgorithm jweAlgorithm() {
        return JWEAlgorithm.ECDH_ES_A256KW;
    }

    /**
     * Returns the wrapped key encryption method.
     */
    default EncryptionMethod encryptionMethod() {
        return EncryptionMethod.A256GCM;
    }
}
