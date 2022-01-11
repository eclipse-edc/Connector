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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.common.ProtocolsSecretToken;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.TopicManager;

import java.util.concurrent.CompletableFuture;

/**
 * Provisions a topic that receives data from a provider runtime.
 */
public class PushStreamProvisioner implements Provisioner<PushStreamResourceDefinition, PushStreamProvisionedResourceDefinition> {
    private final TopicManager topicManager;

    public PushStreamProvisioner(TopicManager topicManager) {
        this.topicManager = topicManager;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof PushStreamResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof PushStreamProvisionedResourceDefinition;
    }

    @Override
    public CompletableFuture<ProvisionResponse> provision(PushStreamResourceDefinition resourceDefinition) {
        return topicManager.provision(resourceDefinition.getTopicName())
                .thenApply(destination -> {
                    var resource = PushStreamProvisionedResourceDefinition.Builder.newInstance()
                            .id(destination.getDestinationName())
                            .transferProcessId(resourceDefinition.getTransferProcessId())
                            .resourceDefinitionId(resourceDefinition.getId())
                            .endpointAddress(resourceDefinition.getEndpointAddress())
                            .destinationName(destination.getDestinationName())
                            .build();

                    var secretToken = new ProtocolsSecretToken(destination.getAccessToken());

                    return ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();
                });
    }

    @Override
    public CompletableFuture<DeprovisionResponse> deprovision(PushStreamProvisionedResourceDefinition provisionedResource) {
        // FIXME
        // topicManager.deprovision(provisionedResource);
        return CompletableFuture.completedFuture(DeprovisionResponse.Builder.newInstance().resource(provisionedResource).build());
    }
}
