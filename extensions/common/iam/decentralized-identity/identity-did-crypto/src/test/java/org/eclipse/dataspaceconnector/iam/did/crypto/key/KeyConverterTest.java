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

package org.eclipse.dataspaceconnector.iam.did.crypto.key;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.KeyType;
import org.eclipse.dataspaceconnector.iam.did.spi.document.EllipticCurvePublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyConverterTest {

    @Test
    void toEcKey_illegalParams() {
        assertThatThrownBy(() -> KeyConverter.toEcKey(null, "some-id")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> KeyConverter.toEcKey(new EllipticCurvePublicKey(), "some-id")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toEcKey_invalidCurve() {
        assertThatThrownBy(() -> KeyConverter.toEcKey(new EllipticCurvePublicKey("foobar", "EC", "asdf", "ASdfkl"), "some-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported curve");
    }

    @Test
    void toEcKey_invalidType_shouldCorrectAutomatically() {
        assertThat(KeyConverter.toEcKey(new EllipticCurvePublicKey("secp256k1", "foobar", "wSwuib0Eyfsvdb_RPpQQLlFoHsQE4TSlFdncLePp6Zg", "uxjZNS8HQ9krKn5ZXpjBtSAAj9FQXSDlHlEMR2YA7Hs"), "some-id"))
                .isNotNull()
                .hasFieldOrPropertyWithValue("kty", KeyType.EC);
    }

    @Test
    void toEcKey_invalidCoordinates() {
        assertThatThrownBy(() -> KeyConverter.toEcKey(new EllipticCurvePublicKey("secp256k1", "EC", "foobar", "uxjZNS8HQ9krKn5ZXpjBtSAAj9FQXSDlHlEMR2YA7Hs"), "some-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid EC JWK: The 'x' and 'y' public coordinates are not on the secp256k1 curve");

        assertThatThrownBy(() -> KeyConverter.toEcKey(new EllipticCurvePublicKey("secp256k1", "EC", "wSwuib0Eyfsvdb_RPpQQLlFoHsQE4TSlFdncLePp6Zg", "foobar"), "some-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid EC JWK: The 'x' and 'y' public coordinates are not on the secp256k1 curve");
    }

    @Test
    void toPublicKeyWrapper() {
        PublicKeyWrapper actual = KeyConverter.toPublicKeyWrapper(new EllipticCurvePublicKey("P-256", "ec", "4mi45pgE5iPdhluNpmtnAFztWi8vxMrDSoXqD5ah2Rk", "FdxTvkrkYtmxPgdmFpxRzZSVvcVUEksSzr1cH_kT58w"), "some-id");
        assertThat(actual).isNotNull().isInstanceOf(EcPublicKeyWrapper.class);
        assertThat(actual.encrypter()).isInstanceOf(ECDHEncrypter.class);
        assertThat(actual.verifier()).isInstanceOf(ECDSAVerifier.class);
        assertThat(actual.jweAlgorithm()).isEqualTo(JWEAlgorithm.ECDH_ES_A256KW);
    }

    @Test
    void toPublicKeyWrapper_illegalKeyType() {
        assertThatThrownBy(() -> KeyConverter.toPublicKeyWrapper(new EllipticCurvePublicKey("P-256", "foobar", "4mi45pgE5iPdhluNpmtnAFztWi8vxMrDSoXqD5ah2Rk", "FdxTvkrkYtmxPgdmFpxRzZSVvcVUEksSzr1cH_kT58w"), "some-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("of type 'EC' can be used at the moment, but 'foobar' was specified!");

    }

    @Test
    void toPublicKeyWrapper_illegalJwkInstance() {
        assertThatThrownBy(() -> KeyConverter.toPublicKeyWrapper(() -> "EC", "test-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Public key has 'kty' = 'EC' but its Java type was");
    }

}
