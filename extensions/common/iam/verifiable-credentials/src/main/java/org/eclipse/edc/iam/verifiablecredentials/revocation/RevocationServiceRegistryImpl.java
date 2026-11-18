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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class RevocationServiceRegistryImpl implements RevocationServiceRegistry {
    private final Map<String, RevocationListService> entries = new HashMap<>();
    private final Monitor monitor;
    private final boolean ignoreUnknownStatusTypes;

    /**
     * Creates a registry that rejects credentials whose {@code credentialStatus} type has no registered {@link RevocationListService}.
     *
     * @param monitor the monitor
     */
    public RevocationServiceRegistryImpl(Monitor monitor) {
        this(monitor, false);
    }

    /**
     * Creates a registry.
     *
     * @param monitor                  the monitor
     * @param ignoreUnknownStatusTypes if {@code true}, a {@code credentialStatus} whose type has no registered {@link RevocationListService}
     *                                 is not checked and is considered valid; if {@code false}, the credential is rejected.
     */
    public RevocationServiceRegistryImpl(Monitor monitor, boolean ignoreUnknownStatusTypes) {
        this.monitor = monitor;
        this.ignoreUnknownStatusTypes = ignoreUnknownStatusTypes;
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

    @Override
    public Result<String> getRevocationStatus(VerifiableCredential credential) {
        return credential.getCredentialStatus()
                .stream()
                .map(credentialStatus -> getRevocationStatusInternal(credentialStatus, credential))
                .reduce((r1, r2) -> {
                    if (r1.succeeded() && r2.succeeded()) {
                        return Result.success(Stream.of(r1.getContent(), r2.getContent()).filter(Objects::nonNull).collect(Collectors.joining(", ")));
                    }
                    return r1.merge(r2);
                })
                .orElse(Result.success(null));
    }

    private Result<String> getRevocationStatusInternal(CredentialStatus credentialStatus, VerifiableCredential credential) {
        return ofNullable(entries.get(credentialStatus.type()))
                .map(service -> service.getStatusPurpose(credential))
                .orElse(Result.success(null));
    }

    private Result<Void> checkRevocation(CredentialStatus credentialStatus) {
        var service = entries.get(credentialStatus.type());
        if (service == null) {
            if (ignoreUnknownStatusTypes) {
                monitor.warning("No revocation service registered for type '%s', will not check revocation.".formatted(credentialStatus.type()));
                return Result.success();
            }
            return Result.failure("No revocation service registered for credential status type '%s'".formatted(credentialStatus.type()));
        }
        return service.checkValidity(credentialStatus);
    }
}
