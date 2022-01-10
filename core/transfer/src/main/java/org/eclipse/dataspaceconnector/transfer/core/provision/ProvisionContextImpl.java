/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.provision;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Provision context backed by a {@link TransferProcessStore}.
 */
public class ProvisionContextImpl implements ProvisionContext {
    private final Consumer<ProvisionedResource> resourceCallback;
    private final BiConsumer<ProvisionedDataDestinationResource, SecretToken> destinationCallback;
    private final BiConsumer<ProvisionedDataDestinationResource, Throwable> deprovisionCallback;

    public ProvisionContextImpl(Consumer<ProvisionedResource> resourceCallback,
                                BiConsumer<ProvisionedDataDestinationResource, SecretToken> destinationCallback,
                                BiConsumer<ProvisionedDataDestinationResource, Throwable> deprovisionCallback) {
        this.resourceCallback = resourceCallback;
        this.destinationCallback = destinationCallback;
        this.deprovisionCallback = deprovisionCallback;
    }

    @Override
    public void callback(ProvisionedResource resource) {
        resourceCallback.accept(resource);
    }

    @Override
    public void callback(ProvisionedDataDestinationResource resource, SecretToken secretToken) {
        destinationCallback.accept(resource, secretToken);
    }

    @Override
    public void deprovisioned(ProvisionedDataDestinationResource resource, Throwable error) {
        deprovisionCallback.accept(resource, error);
    }

}
