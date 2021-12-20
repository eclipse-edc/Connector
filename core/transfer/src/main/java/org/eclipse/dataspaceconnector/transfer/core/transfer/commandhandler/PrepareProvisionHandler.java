package org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler;

import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.PrepareProvision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Provision;

import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;

/**
 * Performs consumer-side or provider side provisioning for a service.
 * <br/>
 * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
 * map involve preprocessing data or other operations.
 */
public class PrepareProvisionHandler implements TransferProcessCommandHandler<PrepareProvision> {
    private final TransferProcessStore transferProcessStore;
    private final ResourceManifestGenerator manifestGenerator;

    public PrepareProvisionHandler(TransferProcessStore transferProcessStore, ResourceManifestGenerator manifestGenerator) {
        this.transferProcessStore = transferProcessStore;
        this.manifestGenerator = manifestGenerator;
    }

    @Override
    public Class<PrepareProvision> handles() {
        return PrepareProvision.class;
    }

    @Override
    public TransferProcessCommandResult handle(PrepareProvision command) {
        TransferProcess process = transferProcessStore.find(command.getId());
        DataRequest dataRequest = process.getDataRequest();
        ResourceManifest manifest;
        if (process.getType() == CONSUMER) {
            // if resources are managed by this connector, generate the manifest; otherwise create an empty one
            manifest = dataRequest.isManagedResources() ? manifestGenerator.generateConsumerManifest(process) : ResourceManifest.Builder.newInstance().build();
        } else {
            manifest = manifestGenerator.generateProviderManifest(process);
        }
        process.transitionProvisioning(manifest);
        transferProcessStore.update(process);

        return new TransferProcessCommandResult(new Provision(process.getId()), listener -> listener::provisioning);
    }

}
