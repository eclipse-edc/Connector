/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyConverter;
import org.eclipse.edc.spi.EdcException;

import java.security.PublicKey;
import java.util.List;

public class TestFunctions {

    public static PublicKey createPublicKey(ECKey signingKey) {
        return KeyConverter.toJavaKeys(List.of(signingKey))
                .stream()
                .filter(k -> k instanceof PublicKey)
                .map(k -> (PublicKey) k)
                .findFirst()
                .orElseThrow(() -> new EdcException("EC Key cannot be converted to a Java PublicKey"));

    }
}
