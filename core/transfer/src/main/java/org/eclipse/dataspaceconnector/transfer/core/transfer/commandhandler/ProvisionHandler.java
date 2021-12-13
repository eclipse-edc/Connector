package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlow;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireTransition;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;

import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;

/**
 * Performs consumer-side or provider side provisioning for a service.
 * <br/>
 * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
 * map involve preprocessing data or other operations.
 */
public class ProvisionHandler implements TransferProcessCommandHandler<Provision> {
    private final TransferProcessStore transferProcessStore;
    private final ResourceManifestGenerator manifestGenerator;
    private final ProvisionManager provisionManager;

    public ProvisionHandler(TransferProcessStore transferProcessStore, ResourceManifestGenerator manifestGenerator, ProvisionManager provisionManager) {
        this.transferProcessStore = transferProcessStore;
        this.manifestGenerator = manifestGenerator;
        this.provisionManager = provisionManager;
    }

    @Override
    public Class<Provision> handles() {
        return Provision.class;
    }

    @Override
    public TransferProcessCommandResult handle(Provision command) {
        TransferProcess process = transferProcessStore.find(command.getId());
        DataRequest dataRequest = process.getDataRequest();
        ResourceManifest manifest;
        TransferProcessCommand nextCommand;
        if (process.getType() == CONSUMER) {
            // if resources are managed by this connector, generate the manifest; otherwise create an empty one
            manifest = dataRequest.isManagedResources() ? manifestGenerator.generateConsumerManifest(process) : ResourceManifest.Builder.newInstance().build();
            nextCommand = new RequireTransition(process.getId());
        } else {
            manifest = manifestGenerator.generateProviderManifest(process);
            nextCommand = new InitiateDataFlow(process.getId());
        }
        process.transitionProvisioning(manifest);
        transferProcessStore.update(process);
        provisionManager.provision(process);
        return new TransferProcessCommandResult(nextCommand, listener -> listener::provisioning);
    }

}
