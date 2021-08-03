package org.eclipse.edc.transfer.demo.protocols.stream;

import org.eclipse.edc.spi.transfer.provision.ProvisionContext;
import org.eclipse.edc.spi.transfer.provision.Provisioner;
import org.eclipse.edc.spi.transfer.response.ResponseStatus;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.edc.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.edc.spi.types.domain.transfer.SecretToken;
import org.eclipse.edc.transfer.demo.protocols.common.ProtocolsSecretToken;
import org.eclipse.edc.transfer.demo.protocols.spi.stream.TopicManager;

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
