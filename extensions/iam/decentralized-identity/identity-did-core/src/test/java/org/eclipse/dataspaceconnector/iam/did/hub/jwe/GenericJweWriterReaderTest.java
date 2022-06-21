/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.hub.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.RsaPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.RsaPublicKeyWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.dataspaceconnector.iam.did.testfixtures.TemporaryKeyLoader.loadKeys;


class GenericJweWriterReaderTest {

    @Test
    void verifyWriteRead() throws Exception {
        var key = loadKeys();
        var privateKey = key.toRSAPrivateKey();
        var publicKey = key.toRSAPublicKey();

        var objectMapper = new ObjectMapper();

        var jwe = new GenericJweWriter()
                .privateKey(new RsaPrivateKeyWrapper(privateKey))
                .publicKey(new RsaPublicKeyWrapper(publicKey))
                .objectMapper(objectMapper)
                .payload(Map.of("foo", "bar"))
                .buildJwe();

        var deserialized = new GenericJweReader().privateKey(new RsaPrivateKeyWrapper(privateKey)).mapper(objectMapper).jwe(jwe).readType(Map.class);
        Assertions.assertEquals("bar", deserialized.get("foo"));
    }
}
