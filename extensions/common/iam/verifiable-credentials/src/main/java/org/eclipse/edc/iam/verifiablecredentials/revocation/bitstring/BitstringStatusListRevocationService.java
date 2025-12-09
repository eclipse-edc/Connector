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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.revocation.BaseRevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.StatusMessage;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.Collection;

import static org.eclipse.edc.spi.result.Result.success;

/**
 * StatusList revocation service implementing the <a href="https://www.w3.org/TR/vc-bitstring-status-list/">BitStringStatusList</a>
 * specification.
 */
public class BitstringStatusListRevocationService extends BaseRevocationListService<BitstringStatusListCredential, BitstringStatusListStatus> {

    public BitstringStatusListRevocationService(ObjectMapper mapper, long cacheValidity, Collection<String> acceptedContentTypes,
                                                EdcHttpClient httpClient, TokenValidationService tokenValidationService,
                                                DidPublicKeyResolver didPublicKeyResolver) {
        super(mapper, cacheValidity, acceptedContentTypes, httpClient, tokenValidationService, didPublicKeyResolver, BitstringStatusListCredential.class);
    }

    @Override
    protected Result<Void> preliminaryChecks(BitstringStatusListStatus credentialStatus) {
        var statusSize = credentialStatus.getStatusSize();
        if (statusSize != 1) { //todo: support more statusSize entries in the future
            return Result.failure("Unsupported statusSize: currently only statusSize = 1 is supported. The VC contained statusSize = %d".formatted(statusSize));
        }
        return success();
    }

    @Override
    protected Result<String> getStatusEntryValue(BitstringStatusListStatus credentialStatus) {
        var bitStringCredentialResult = getCredential(credentialStatus.getStatusListCredential());
        if (bitStringCredentialResult.failed()) {
            return bitStringCredentialResult.mapEmpty();
        }
        var bitStringCredential = bitStringCredentialResult.getContent();

        var bitString = bitStringCredential.encodedList();

        var compressedBitstring = BitString.Parser.newInstance().parse(bitString);
        if (compressedBitstring.failed()) {
            return compressedBitstring.mapEmpty();
        }
        var bitstring = compressedBitstring.getContent();

        // todo: check that encodedList / statusSize == minimumLength (defaults to 131_072 = encodedList minimum length in bits),
        // otherwise raise error
        // todo: how to determine minimumLength? via config?

        var statusFlag = bitstring.get(credentialStatus.getStatusListIndex());

        var statusPurpose = credentialStatus.getStatusListPurpose();
        // if the purpose is "message", we need to check the statusMessage object for the actual string
        if (statusPurpose.equalsIgnoreCase("message")) {
            var statusString = statusFlag ? "0x1" : "0x0"; //todo: change this when statusSize > 1 is supported
            statusPurpose = credentialStatus.getStatusMessage().stream().filter(sm -> sm.status().equals(statusString)).map(StatusMessage::message).findAny().orElse(statusPurpose);

            return success(statusPurpose);
        } else if (statusFlag) {
            // currently, this supports only a statusSize of 1
            return success(statusPurpose);
        }

        return success(null);
    }

    @Override
    protected Result<Void> validateStatusPurpose(BitstringStatusListStatus credentialStatus) {
        var statusPurpose = credentialStatus.getStatusListPurpose();

        var credentialUrl = credentialStatus.getStatusListCredential();
        var statusListCredential = getCredential(credentialUrl);
        if (statusListCredential.failed()) {
            return statusListCredential.mapEmpty();
        }
        var credentialStatusPurpose = statusListCredential.getContent().statusPurpose();

        if (!statusPurpose.equalsIgnoreCase(credentialStatusPurpose)) {
            return Result.failure("Credential's statusPurpose value must match the statusPurpose of the Bitstring Credential: '%s' != '%s'".formatted(statusPurpose, credentialStatusPurpose));
        }

        return success();
    }

    @Override
    protected int getStatusIndex(BitstringStatusListStatus credentialStatus) {
        return credentialStatus.getStatusListIndex();
    }

    @Override
    protected BitstringStatusListStatus getCredentialStatus(CredentialStatus credentialStatus) {
        return BitstringStatusListStatus.from(credentialStatus);
    }

}
