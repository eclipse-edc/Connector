package com.microsoft.dagx.transfer.core.provision;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Default provision manager. Invoke {@link #start(TransferProcessStore)} to initialize an instance.
 */
public class ProvisionManagerImpl implements ProvisionManager {
    private Vault vault;
    private Monitor monitor;
    private TransferProcessStore processStore;
    private List<Provisioner<?, ?>> provisioners = new ArrayList<>();

    public ProvisionManagerImpl(Vault vault, Monitor monitor) {
        this.vault = vault;
        this.monitor = monitor;
    }

    public void start(TransferProcessStore processStore) {
        this.processStore = processStore;
        var context = new ProvisionContextImpl(this.processStore, this::onResource);
        provisioners.forEach(provisioner -> provisioner.initialize(context));
    }

    @Override
    public <RD extends ResourceDefinition, PR extends ProvisionedResource> void register(Provisioner<RD, PR> provisioner) {
        provisioners.add(provisioner);
    }

    @Override
    public void provision(TransferProcess process) {
        if (process.getResourceManifest().getDefinitions().isEmpty()) {
            // no resources to provision, advance state
            process.transitionProvisioned();
            processStore.update(process);
        }
        for (ResourceDefinition definition : process.getResourceManifest().getDefinitions()) {
            Provisioner<ResourceDefinition, ?> chosenProvisioner = getProvisioner(definition);
            var status = chosenProvisioner.provision(definition);
        }
    }

    @Override
    public void deprovision(TransferProcess process) {
        for (ProvisionedResource definition : process.getProvisionedResourceSet().getResources()) {
            Provisioner<?, ProvisionedResource> chosenProvisioner = getProvisioner(definition);
            chosenProvisioner.deprovision(definition);
        }
    }

    void onResource(ProvisionedResource provisionedResource, DestinationSecretToken secretToken) {
        var processId = provisionedResource.getTransferProcessId();
        var transferProcess = processStore.find(processId);
        if (transferProcess == null) {
            var resourceId = provisionedResource.getResourceDefinitionId();
            monitor.severe(format("Error received when provisioning resource %s Process id not found for: %s", resourceId, processId));
            return;
        }
        transferProcess.addProvisionedResource(provisionedResource);

        if (secretToken != null) {
            // TODO we should probably create a hierarchy for keys
            vault.storeSecret(provisionedResource.getId(), secretToken.getToken());
        }

        if (provisionedResource.isError()) {
            var resourceId = provisionedResource.getResourceDefinitionId();
            monitor.severe(format("Error provisioning resource %s for process %s: %s", resourceId, processId, provisionedResource.getErrorMessage()));
            processStore.update(transferProcess);
            return;
        }

        if (TransferProcessStates.ERROR.code() != transferProcess.getState() && transferProcess.provisioningComplete()) {
            // TODO If all resources provisioned, delete scratch data
            transferProcess.transitionProvisioned();
        }
        processStore.update(transferProcess);
    }

    @NotNull
    private Provisioner<ResourceDefinition, ?> getProvisioner(ResourceDefinition definition) {
        Provisioner<ResourceDefinition, ?> provisioner = null;
        for (Provisioner<?, ?> candidate : provisioners) {
            if (candidate.canProvision(definition)) {
                //noinspection unchecked
                provisioner = (Provisioner<ResourceDefinition, ?>) candidate;
                break;
            }
        }
        if (provisioner == null) {
            throw new DagxException("Unknown provision type" + definition.getClass().getName());
        }
        return provisioner;
    }

    @NotNull
    private Provisioner<?, ProvisionedResource> getProvisioner(ProvisionedResource provisionedResource) {
        Provisioner<?, ProvisionedResource> provisioner = null;
        for (Provisioner<?, ?> candidate : provisioners) {
            if (candidate.canDeprovision(provisionedResource)) {
                //noinspection unchecked
                provisioner = (Provisioner<?, ProvisionedResource>) candidate;
                break;
            }
        }
        if (provisioner == null) {
            throw new DagxException("Unknown provision type" + provisionedResource.getClass().getName());
        }
        return provisioner;
    }


}
