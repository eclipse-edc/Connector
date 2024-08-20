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

package org.eclipse.edc.iam.verifiablecredentials.revocation.bistring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.collection.Cache;

import java.io.IOException;
import java.net.URI;

public class BitstringStatusListRevocationService implements RevocationListService {
    private final ObjectMapper objectMapper;
    private final Cache<String, VerifiableCredential> cache;

    public BitstringStatusListRevocationService(ObjectMapper mapper) {
        this.objectMapper = mapper.copy()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY) // technically, credential subjects and credential status can be objects AND Arrays
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // let's make sure this is disabled, because the "@context" would cause problems

        cache = new Cache<>(this::downloadStatusListCredential, 5 * 1000 * 60); //todo: change to dynamic TTL!
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

    private Result<Void> checkStatus(BitstringStatusListStatus bitstringStatus) {
        return Result.failure("not implemented");
    }

    private VerifiableCredential downloadStatusListCredential(String credentialUrl) {
        try {
            return objectMapper.readValue(URI.create(credentialUrl).toURL(), VerifiableCredential.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
