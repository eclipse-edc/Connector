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

package org.eclipse.edc.iam.verifiablecredentials;

import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

public class RevocationServiceRegistryImpl implements RevocationServiceRegistry {
    private final Map<String, RevocationListService> entries = new HashMap<>();

    @Override
    public void addService(String statusListType, RevocationListService service) {
        entries.put(statusListType, service);
    }

    @Override
    public Result<Void> checkValidity(VerifiableCredential credential) {
        return credential.getCredentialStatus().stream()
                .map(this::checkRevocation)
                .reduce(Result::merge)
                .orElse(Result.failure("Could not check the validity of the credential with ID '%s'".formatted(credential.getId())));
    }

    private Result<Void> checkRevocation(CredentialStatus credentialStatus) {
        var service = entries.get(credentialStatus.type());
        if (service == null) {
            return Result.failure("No revocation service registered for type '%s'.".formatted(credentialStatus.type()));
        }
        return service.checkValidity(credentialStatus);
    }
}
