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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.StatusMessage;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.Cache;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.stream.Collectors;

import static org.eclipse.edc.spi.result.Result.success;

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
        if (credential.getCredentialStatus().isEmpty()) {
            return success(null);
        }

        var res = credential.getCredentialStatus().stream()
                .map(BitstringStatusListStatus::parse)
                .map(this::getStatusInternal)
                .collect(Collectors.groupingBy(AbstractResult::succeeded));

        if (res.containsKey(false)) { //if any failed
            return Result.failure(res.get(false).stream().map(AbstractResult::getFailureDetail).toList());
        }

        var list = res.get(true).stream()
                .filter(r -> r.getContent() != null)
                .map(AbstractResult::getContent).toList();

        // there could be several statusPurposes. collect them in a list.
        return list.isEmpty() ? success(null) : success(String.join(", ", list));

    }

    private Result<Void> checkStatus(BitstringStatusListStatus status) {
        var credentialIndex = status.getStatusListIndex();

        return getStatusInternal(status)
                .compose(purpose -> purpose != null ?
                        Result.failure("Credential status is '%s', status at index %d is '1'".formatted(purpose, credentialIndex)) :
                        success());
    }

    private Result<String> getStatusInternal(BitstringStatusListStatus status) {
        var credentialIndex = status.getStatusListIndex();

        var statusSize = status.getStatusSize();
        if (statusSize != 1) { //todo: support more statusSize entries in the future
            return Result.failure("Unsupported statusSize: currently only statusSize = 1 is supported. The VC contained statusSize = %d".formatted(statusSize));
        }

        var statusPurpose = status.getStatusListPurpose();
        var credentialUrl = status.getStatusListCredential();
        var credential = cache.get(credentialUrl);
        var bitStringCredential = BitstringStatusListCredential.parse(credential);

        var credentialStatusPurpose = bitStringCredential.statusPurpose();

        if (!statusPurpose.equalsIgnoreCase(credentialStatusPurpose)) {
            return Result.failure("Credential's statusPurpose value must match the statusPurpose of the Bitstring Credential: '%s' != '%s'".formatted(statusPurpose, credentialStatusPurpose));
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
        var bitstring = compressedBitstring.getContent();

        //todo: check that encodedList / statusSize == minimumLength (defaults to 131_072), otherwise raise error
        //todo: how to determine minimumLength? via config?

        var statusFlag = bitstring.get(credentialIndex);

        // if the purpose is "message", we need to check the statusMessage object for the actual string
        if (statusPurpose.equalsIgnoreCase("message")) {
            var statusString = statusFlag ? "0x1" : "0x0"; //todo: change this when statusSize > 1 is supported
            statusPurpose = status.getStatusMessage().stream().filter(sm -> sm.status().equals(statusString)).map(StatusMessage::message).findAny().orElse(statusPurpose);

            return success(statusPurpose);
        } else if (statusFlag) {
            // currently, this supports only a statusSize of 1
            return success(statusPurpose);
        }

        return success(null);
    }

    private VerifiableCredential downloadStatusListCredential(String credentialUrl) {
        try {
            return objectMapper.readValue(URI.create(credentialUrl).toURL(), VerifiableCredential.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
