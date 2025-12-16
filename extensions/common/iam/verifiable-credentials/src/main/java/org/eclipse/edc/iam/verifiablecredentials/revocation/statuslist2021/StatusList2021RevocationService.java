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

package org.eclipse.edc.iam.verifiablecredentials.revocation.statuslist2021;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.revocation.BaseRevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Credential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.Collection;

import static org.eclipse.edc.spi.result.Result.success;


/**
 * StatusList revocation service implementing the <a href="https://w3c.github.io/cg-reports/credentials/CG-FINAL-vc-status-list-2021-20230102/">StatusList2021</a>
 * specification.
 */
public class StatusList2021RevocationService extends BaseRevocationListService<StatusList2021Credential, StatusList2021Status> {

    public StatusList2021RevocationService(ObjectMapper objectMapper, long cacheValidity, Collection<String> acceptedContentTypes,
                                           EdcHttpClient httpClient, TokenValidationService tokenValidationService,
                                           DidPublicKeyResolver didPublicKeyResolver) {
        super(objectMapper, cacheValidity, acceptedContentTypes, httpClient, tokenValidationService, didPublicKeyResolver, StatusList2021Credential.class);
    }

    @Override
    protected StatusList2021Status getCredentialStatus(CredentialStatus credentialStatus) {
        return StatusList2021Status.from(credentialStatus);
    }

    @Override
    protected Result<String> getStatusEntryValue(StatusList2021Status credentialStatus) {
        var index = credentialStatus.getStatusListIndex();
        var slCredUrl = credentialStatus.getStatusListCredential();
        var credential = getCredential(slCredUrl);
        if (credential.failed()) {
            return credential.mapEmpty();
        }

        var bitStringResult = BitString.Parser.newInstance().parse(credential.getContent().encodedList());

        if (bitStringResult.failed()) {
            return bitStringResult.mapEmpty();
        }
        var bitString = bitStringResult.getContent();

        // check that the value at index in the bitset is "1"
        if (bitString.get(index)) {
            return success(credentialStatus.getStatusListPurpose());
        }
        return success(null);
    }

    @Override
    protected Result<Void> validateStatusPurpose(StatusList2021Status credentialStatus) {
        var slCred = getCredential(credentialStatus.getStatusListCredential());
        if (slCred.failed()) {
            return slCred.mapEmpty();
        }

        // check that the "statusPurpose" values match
        var purpose = credentialStatus.getStatusListPurpose();
        var slCredPurpose = slCred.getContent().statusPurpose();
        if (!purpose.equalsIgnoreCase(slCredPurpose)) {
            return Result.failure("Credential's statusPurpose value must match the status list's purpose: '%s' != '%s'".formatted(purpose, slCredPurpose));
        }

        return success();
    }

    @Override
    protected int getStatusIndex(StatusList2021Status credentialStatus) {
        return credentialStatus.getStatusListIndex();
    }
}
