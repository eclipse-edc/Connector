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

package org.eclipse.edc.iam.did.crypto.key;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeyConverterTest {

    @Test
    void toPublicKeyWrapper_unknownKeyType() {
        var result = KeyConverter.toPublicKeyWrapper(Map.of("kty", "unknonwn"), "some-id");

        assertThat(result.failed()).isTrue();
    }

    @Test
    void toPublicKeyWrapper_ecKey_success() {
        var jwk = new HashMap<String, Object>();
        jwk.put("kty", "EC");
        jwk.put("crv", "P-256");
        jwk.put("x", "4mi45pgE5iPdhluNpmtnAFztWi8vxMrDSoXqD5ah2Rk");
        jwk.put("y", "FdxTvkrkYtmxPgdmFpxRzZSVvcVUEksSzr1cH_kT58w");

        var result = KeyConverter.toPublicKeyWrapper(jwk, "some-id");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isInstanceOf(EcPublicKeyWrapper.class);
    }

    @Test
    void toPublicKeyWrapper_rsaKey_success() {
        var jwk = new HashMap<String, Object>();
        jwk.put("kty", "RSA");
        jwk.put("n", "wyUT5cJNKlXI7sfQW59WTFySiAc_I9LYHIQd2JBY0nwAjoFkRQOqrEYVbk42jaLZrIfz9gi5AzKuQ27QNd4zVQ");
        jwk.put("e", "AQAB");

        var result = KeyConverter.toPublicKeyWrapper(jwk, "some-id");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isInstanceOf(RsaPublicKeyWrapper.class);
    }
}
