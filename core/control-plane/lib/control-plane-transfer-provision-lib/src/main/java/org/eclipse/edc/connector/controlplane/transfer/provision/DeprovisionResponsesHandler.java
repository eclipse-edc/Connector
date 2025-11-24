/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transfer.provision;

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;

public class DeprovisionResponsesHandler implements ResponsesHandler<StatusResult<DeprovisionedResource>> {

    private final TransferProcessObservable observable;
    private final Monitor monitor;
    private final Vault vault;

    public DeprovisionResponsesHandler(TransferProcessObservable observable, Monitor monitor, Vault vault) {
        this.observable = observable;
        this.monitor = monitor;
        this.vault = vault;
    }

    @Override
    public boolean handle(TransferProcess transferProcess, List<StatusResult<DeprovisionedResource>> responses) {
        var validationResult = responses.stream()
                .filter(AbstractResult::failed)
                .map(this::toFatalError)
                .filter(AbstractResult::failed)
                .reduce(Result::merge)
                .orElse(Result.success());

        if (validationResult.failed()) {
            var message = format("Transitioning transfer process %s failed to deprovision. Errors: \n%s", transferProcess.getId(), validationResult.getFailureDetail());
            transitionToDeprovisioningError(transferProcess, message);
            return true;
        }

        var deprovisionResponses = responses.stream()
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());

        handleDeprovisionResponses(transferProcess, deprovisionResponses);

        return true;
    }

    @Override
    public void postActions(TransferProcess transferProcess) {
        if (transferProcess.currentStateIsOneOf(DEPROVISIONED)) {
            observable.invokeForEach(l -> l.deprovisioned(transferProcess));
        } else if (transferProcess.currentStateIsOneOf(DEPROVISIONING_REQUESTED)) {
            observable.invokeForEach(l -> l.deprovisioningRequested(transferProcess));
        }
    }

    private void handleDeprovisionResponses(TransferProcess transferProcess, List<DeprovisionedResource> results) {
        results.stream()
                .map(deprovisionedResource -> {
                    var provisionedResource = transferProcess.getProvisionedResource(deprovisionedResource.getProvisionedResourceId());
                    if (provisionedResource == null) {
                        monitor.severe("Received a deprovision result for a provisioned resource that was not found. Skipping.");
                        return null;
                    }

                    if (provisionedResource.hasToken() && provisionedResource instanceof ProvisionedDataAddressResource) {
                        removeDeprovisionedSecrets(transferProcess.getParticipantContextId(), (ProvisionedDataAddressResource) provisionedResource, transferProcess.getId());
                    }
                    return deprovisionedResource;
                })
                .filter(Objects::nonNull)
                .forEach(transferProcess::addDeprovisionedResource);

        if (transferProcess.deprovisionComplete()) {
            transferProcess.transitionDeprovisioned();
            observable.invokeForEach(l -> l.preDeprovisioned(transferProcess));
        } else if (results.stream().anyMatch(DeprovisionedResource::isInProcess)) {
            transferProcess.transitionDeprovisioningRequested();
        }
    }

    private void removeDeprovisionedSecrets(String participantContextId, ProvisionedDataAddressResource provisionedResource, String transferProcessId) {
        var keyName = provisionedResource.getResourceName();
        var result = vault.deleteSecret(participantContextId, keyName);
        if (result.failed()) {
            monitor.severe(format("Error deleting secret from vault with key %s for transfer process %s: \n %s",
                    keyName, transferProcessId, join("\n", result.getFailureMessages())));
        }
    }

    private void transitionToDeprovisioningError(TransferProcess transferProcess, String message) {
        monitor.severe(message);
        transferProcess.transitionDeprovisioned(message);
        observable.invokeForEach(l -> l.preDeprovisioned(transferProcess));
    }
}
