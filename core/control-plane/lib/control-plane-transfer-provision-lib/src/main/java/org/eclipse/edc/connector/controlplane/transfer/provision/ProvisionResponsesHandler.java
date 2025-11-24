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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;

/**
 * Handles provisioning results, wrapping all the logic needed.
 * This component does not interact with the store.
 */
public class ProvisionResponsesHandler implements ResponsesHandler<StatusResult<ProvisionResponse>> {

    private final TransferProcessObservable observable;
    private final Monitor monitor;
    private final Vault vault;
    private final TypeManager typeManager;

    public ProvisionResponsesHandler(TransferProcessObservable observable, Monitor monitor, Vault vault, TypeManager typeManager) {
        this.observable = observable;
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
    }

    @Override
    public boolean handle(TransferProcess transferProcess, List<StatusResult<ProvisionResponse>> responses) {
        if (transferProcess.getState() > PROVISIONED.code()) {
            return false;
        }

        responses.stream()
                .map(result -> result.succeeded()
                        ? storeProvisionedSecrets(transferProcess.getParticipantContextId(), transferProcess.getId(), result.getContent())
                        : toFatalError(result)
                )
                .filter(AbstractResult::failed)
                .reduce(Result::merge)
                .orElse(Result.success())
                .onSuccess(v -> {
                    var provisionResponses = responses.stream()
                            .filter(AbstractResult::succeeded)
                            .map(AbstractResult::getContent)
                            .collect(Collectors.toList());

                    handleProvisionResponses(transferProcess, provisionResponses);
                })
                .onFailure(failure -> {
                    var message = format("Terminating transfer process %s due to fatal provisioning errors: %s", transferProcess.getId(), failure.getFailureDetail());
                    monitor.warning(message);
                    if (transferProcess.getType() == PROVIDER) {
                        transferProcess.transitionTerminating(message);
                    } else {
                        monitor.warning(message);
                        transferProcess.transitionTerminated(message);
                    }
                });

        return true;
    }

    @Override
    public void postActions(TransferProcess transferProcess) {
        if (transferProcess.currentStateIsOneOf(PROVISIONED)) {
            observable.invokeForEach(l -> l.provisioned(transferProcess));
        } else if (transferProcess.currentStateIsOneOf(PROVISIONING_REQUESTED)) {
            observable.invokeForEach(l -> l.provisioningRequested(transferProcess));
        } else if (transferProcess.currentStateIsOneOf(TERMINATED)) {
            observable.invokeForEach(l -> l.terminated(transferProcess));
        }
    }

    private void handleProvisionResponses(TransferProcess transferProcess, List<ProvisionResponse> responses) {
        responses.stream()
                .map(response -> {
                    var provisionedResource = response.getResource();

                    if (provisionedResource instanceof ProvisionedDataAddressResource dataAddressResource) {
                        var dataAddress = dataAddressResource.getDataAddress();
                        var secretToken = response.getSecretToken();
                        if (secretToken != null) {
                            var keyName = dataAddressResource.getResourceName();
                            dataAddress.setKeyName(keyName);
                        }

                        if (dataAddressResource instanceof ProvisionedDataDestinationResource) {
                            // a data destination was provisioned by a consumer
                            transferProcess.updateDestination(dataAddress);
                        } else if (dataAddressResource instanceof ProvisionedContentResource) {
                            // content for the data transfer was provisioned by the provider
                            transferProcess.setContentDataAddress(dataAddress);
                        }
                    }

                    return provisionedResource;
                })
                .filter(Objects::nonNull)
                .forEach(transferProcess::addProvisionedResource);

        if (transferProcess.provisioningComplete()) {
            transferProcess.transitionProvisioned();
            observable.invokeForEach(l -> l.preProvisioned(transferProcess));
        } else if (responses.stream().anyMatch(ProvisionResponse::isInProcess)) {
            transferProcess.transitionProvisioningRequested();
        }
    }

    @NotNull
    private Result<Void> storeProvisionedSecrets(String participantContextId, String transferProcessId, ProvisionResponse response) {
        var resource = response.getResource();

        if (resource instanceof ProvisionedDataAddressResource dataAddressResource) {
            var secretToken = response.getSecretToken();
            if (secretToken != null) {
                var keyName = dataAddressResource.getResourceName();
                var secretResult = vault.storeSecret(participantContextId, keyName, typeManager.writeValueAsString(secretToken));
                if (secretResult.failed()) {
                    return Result.failure(format("Error storing secret in vault with key %s for transfer process %s: \n %s",
                            keyName, transferProcessId, join("\n", secretResult.getFailureMessages())));
                }
            }
        }

        return Result.success();
    }
}
