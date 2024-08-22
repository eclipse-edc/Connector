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

package org.eclipse.edc.iam.verifiablecredentials.revocation;

import org.eclipse.edc.iam.verifiablecredentials.spi.RevocationListService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;

public class RevocationServiceRegistryImpl implements RevocationServiceRegistry {
    private final Map<String, RevocationListService> entries = new HashMap<>();
    private final Monitor monitor;

    public RevocationServiceRegistryImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void addService(String statusListType, RevocationListService service) {
        entries.put(statusListType, service);
    }

    @Override
    public Result<Void> checkValidity(VerifiableCredential credential) {
        return credential.getCredentialStatus()
                .stream()
                .map(this::checkRevocation)
                .reduce(Result::merge)
                .orElse(Result.success());
    }

    private Result<Void> checkRevocation(CredentialStatus credentialStatus) {
        var service = entries.get(credentialStatus.type());
        if (service == null) {
            monitor.warning("No revocation service registered for type '%s', will not check revocation.".formatted(credentialStatus.type()));
            return Result.success();
        }
        return service.checkValidity(credentialStatus);
    }
}
