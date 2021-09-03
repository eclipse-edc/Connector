/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ion.model;

import com.nimbusds.jose.jwk.JWK;

public class JwkKeyPair {
    private JWK privateKey;
    private JWK publicKey;

    public static JwkKeyPair from(JWK jwk) {
        var pair = new JwkKeyPair();
        pair.privateKey = jwk;
        pair.publicKey = jwk.toPublicJWK();
        return pair;
    }

    public JWK getPrivateKey() {
        return privateKey;
    }

    public JWK getPublicKey() {
        return publicKey;
    }
}
