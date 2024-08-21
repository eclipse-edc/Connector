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
import org.eclipse.edc.iam.verifiablecredentials.revocation.BaseRevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Credential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.spi.result.Result.success;


/**
 * StatusList revocation service implementing the <a href="https://w3c.github.io/cg-reports/credentials/CG-FINAL-vc-status-list-2021-20230102/">StatusList2021</a>
 * specification.
 */
public class StatusList2021RevocationService extends BaseRevocationListService {

    public StatusList2021RevocationService(ObjectMapper objectMapper, long cacheValidity) {
        super(objectMapper, cacheValidity);
    }

    @Override
    protected Result<String> getStatusEntryValue(CredentialStatus credentialStatus) {
        var status = StatusList2021Status.parse(credentialStatus);
        var index = status.getStatusListIndex();
        var slCredUrl = status.getStatusListCredential();
        var credential = getCredential(slCredUrl);
        var slCred = StatusList2021Credential.parse(credential);


        var bitStringResult = BitString.Parser.newInstance().parse(slCred.encodedList());

        if (bitStringResult.failed()) {
            return bitStringResult.mapEmpty();
        }
        var bitString = bitStringResult.getContent();

        // check that the value at index in the bitset is "1"
        if (bitString.get(index)) {
            return success(status.getStatusListPurpose());
        }
        return success(null);
    }

    @Override
    protected Result<Void> validateStatusPurpose(CredentialStatus credentialStatus) {
        var status2021 = StatusList2021Status.parse(credentialStatus);
        var slCred = StatusList2021Credential.parse(getCredential(status2021.getStatusListCredential()));

        // check that the "statusPurpose" values match
        var purpose = status2021.getStatusListPurpose();
        var slCredPurpose = slCred.statusPurpose();
        if (!purpose.equalsIgnoreCase(slCredPurpose)) {
            return Result.failure("Credential's statusPurpose value must match the status list's purpose: '%s' != '%s'".formatted(purpose, slCredPurpose));
        }

        return success();
    }

    @Override
    protected int getStatusIndex(CredentialStatus credentialStatus) {
        return StatusList2021Status.parse(credentialStatus).getStatusListIndex();
    }
}
