/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.verifiablecredentials.revocation.bitstring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.Cache;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

public class BitstringStatusListRevocationService implements RevocationListService {
    private final ObjectMapper objectMapper;
    private final Cache<String, VerifiableCredential> cache;

    public BitstringStatusListRevocationService(ObjectMapper mapper, long cacheValidity) {
        this.objectMapper = mapper.copy()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // technically, credential subjects and credential status can be objects AND Arrays
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // let's make sure this is disabled, because the "@context" would cause problems

        cache = new Cache<>(this::downloadStatusListCredential, cacheValidity);
    }

    @Override
    public Result<Void> checkValidity(CredentialStatus status) {
        var bitstringStatus = BitstringStatusListStatus.parse(status);
        return checkStatus(bitstringStatus);
    }

    @Override
    public Result<String> getStatusPurpose(VerifiableCredential credential) {
        return null;
    }

    private Result<Void> checkStatus(BitstringStatusListStatus status) {
        var index = status.getStatusListIndex();
        var slCredUrl = status.getStatusListCredential();
        var credential = cache.get(slCredUrl);
        var bitStringCredential = BitstringStatusListCredential.parse(credential);

        var statusPurpose = status.getStatusListPurpose();
        var statusSize = status.getStatusSize();
        var credentialStatusPurpose = bitStringCredential.statusPurpose();

        if (!statusPurpose.equalsIgnoreCase(credentialStatusPurpose)) {
            return Result.failure("Credential's statusPurpose value must match statusPurpose of the Bitstring Credential: '%s' != '%s'".formatted(statusPurpose, credentialStatusPurpose));
        }
        var bitString = bitStringCredential.encodedList();
        var decoder = Base64.getDecoder();
        if (bitString.charAt(0) == 'u') { // base64 url
            decoder = Base64.getUrlDecoder();
            bitString = bitString.substring(1); //chop off header
        } else if (bitString.charAt(0) == 'z') { //base58btc
            return Result.failure("The encoded list contains a Base58-BTC encoding header, which is not supported.");
        }

        var compressedBitstring = BitString.Parser.newInstance().decoder(decoder).parse(bitString);
        if (compressedBitstring.failed()) {
            return compressedBitstring.mapEmpty();
        }
        var credentialIndex = status.getStatusListIndex();
        var bitstring = compressedBitstring.getContent();

        //todo: check that encodedList / statusSize == minimumLength (defaults to 131_072), otherwise raise error

        return Result.success();
    }

    private VerifiableCredential downloadStatusListCredential(String credentialUrl) {
        try {
            return objectMapper.readValue(URI.create(credentialUrl).toURL(), VerifiableCredential.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
