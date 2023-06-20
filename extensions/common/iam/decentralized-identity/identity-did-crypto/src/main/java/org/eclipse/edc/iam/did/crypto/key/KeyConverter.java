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

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKParameterNames;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class KeyConverter {

    private KeyConverter() {
    }

    /**
     * Convert a JWK representation into a Public Key.
     *
     * @param jwk jwk representation of the public key
     * @param id  An arbitrary ID that serves as 'kid' property
     * @return A {@link PublicKeyWrapper}
     */
    public static @NotNull Result<PublicKeyWrapper> toPublicKeyWrapper(Map<String, Object> jwk, String id) {
        // set the key id to provided value;
        var params = new HashMap<>(jwk);
        params.put(JWKParameterNames.KEY_ID, id);
        return parse(params).compose(KeyConverter::toWrapper);
    }

    private static Result<JWK> parse(Map<String, Object> jwk) {
        try {
            return Result.success(JWK.parse(jwk));
        } catch (ParseException e) {
            return Result.failure("Failed to parse key: " + e);
        }
    }

    private static Result<PublicKeyWrapper> toWrapper(JWK publicKey) {
        if (publicKey instanceof ECKey) {
            return Result.success(new EcPublicKeyWrapper(publicKey.toECKey()));
        } else if (publicKey instanceof RSAKey) {
            return Result.success(new RsaPublicKeyWrapper(publicKey.toRSAKey()));
        }
        return Result.failure("Jwk public key type not supported: " + publicKey.getClass().getName());
    }
}
