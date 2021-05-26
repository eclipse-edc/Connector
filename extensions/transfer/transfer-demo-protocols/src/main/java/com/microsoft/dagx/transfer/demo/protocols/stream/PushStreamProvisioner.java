package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.SecretToken;
import com.microsoft.dagx.transfer.demo.protocols.common.ProtocolsSecretToken;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.TopicManager;

/**
 * Provisions a topic that receives data from a provider runtime.
 */
public class PushStreamProvisioner implements Provisioner<PushStreamResourceDefinition, PushStreamProvisionedResourceDefinition> {
    private TopicManager topicManager;

    private ProvisionContext context;

    public PushStreamProvisioner(TopicManager topicManager) {
        this.topicManager = topicManager;
    }

    @Override
    public void initialize(ProvisionContext context) {
        this.context = context;
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
    public ResponseStatus provision(PushStreamResourceDefinition resourceDefinition) {
        topicManager.provision(resourceDefinition.getTopicName()).whenComplete((destination, e) -> {
            var transferProcessId = resourceDefinition.getTransferProcessId();
            var provisionedResource = PushStreamProvisionedResourceDefinition.Builder.newInstance()
                    .id(destination.getDestinationName())
                    .transferProcessId(transferProcessId)
                    .resourceDefinitionId(resourceDefinition.getId())
                    .endpointAddress(resourceDefinition.getEndpointAddress())
                    .destinationName(destination.getDestinationName())
                    .build();
            SecretToken secretToken = new ProtocolsSecretToken(destination.getAccessToken());

            context.callback(provisionedResource, secretToken);
        });
        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(PushStreamProvisionedResourceDefinition provisionedResource) {
        // FIXME
        // topicManager.deprovision(provisionedResource);
        return ResponseStatus.OK;
    }
}
