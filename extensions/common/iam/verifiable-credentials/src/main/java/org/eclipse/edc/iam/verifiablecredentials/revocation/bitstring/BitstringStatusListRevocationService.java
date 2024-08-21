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
import org.eclipse.edc.iam.verifiablecredentials.revocation.BaseRevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.StatusMessage;
import org.eclipse.edc.spi.result.Result;

import java.util.Base64;

import static org.eclipse.edc.spi.result.Result.success;

/**
 * StatusList revocation service implementing the <a href="https://www.w3.org/TR/vc-bitstring-status-list/">BitStringStatusList</a>
 * specification.
 */
public class BitstringStatusListRevocationService extends BaseRevocationListService {

    public BitstringStatusListRevocationService(ObjectMapper mapper, long cacheValidity) {
        super(mapper, cacheValidity);
    }

    @Override
    protected Result<Void> preliminaryChecks(CredentialStatus credentialStatus) {
        var status = BitstringStatusListStatus.parse(credentialStatus);
        var statusSize = status.getStatusSize();
        if (statusSize != 1) { //todo: support more statusSize entries in the future
            return Result.failure("Unsupported statusSize: currently only statusSize = 1 is supported. The VC contained statusSize = %d".formatted(statusSize));
        }
        return success();
    }

    @Override
    protected Result<String> getStatusEntryValue(CredentialStatus credentialStatus) {
        var bitstringStatus = BitstringStatusListStatus.parse(credentialStatus);
        var bitStringCredential = BitstringStatusListCredential.parse(getCredential(bitstringStatus.getStatusListCredential()));

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

        var statusFlag = bitstring.get(bitstringStatus.getStatusListIndex());

        var statusPurpose = bitstringStatus.getStatusListPurpose();
        // if the purpose is "message", we need to check the statusMessage object for the actual string
        if (statusPurpose.equalsIgnoreCase("message")) {
            var statusString = statusFlag ? "0x1" : "0x0"; //todo: change this when statusSize > 1 is supported
            statusPurpose = bitstringStatus.getStatusMessage().stream().filter(sm -> sm.status().equals(statusString)).map(StatusMessage::message).findAny().orElse(statusPurpose);

            return success(statusPurpose);
        } else if (statusFlag) {
            // currently, this supports only a statusSize of 1
            return success(statusPurpose);
        }

        return success(null);
    }

    @Override
    protected Result<Void> validateStatusPurpose(CredentialStatus credentialStatus) {
        var bitstringStatus = BitstringStatusListStatus.parse(credentialStatus);
        var statusPurpose = bitstringStatus.getStatusListPurpose();

        var credentialUrl = bitstringStatus.getStatusListCredential();
        var credential = getCredential(credentialUrl);
        var bitStringCredential = BitstringStatusListCredential.parse(credential);
        var credentialStatusPurpose = bitStringCredential.statusPurpose();

        if (!statusPurpose.equalsIgnoreCase(credentialStatusPurpose)) {
            return Result.failure("Credential's statusPurpose value must match the statusPurpose of the Bitstring Credential: '%s' != '%s'".formatted(statusPurpose, credentialStatusPurpose));
        }

        return success();
    }

    @Override
    protected int getStatusIndex(CredentialStatus credentialStatus) {
        return BitstringStatusListStatus.parse(credentialStatus).getStatusListIndex();
    }

}
