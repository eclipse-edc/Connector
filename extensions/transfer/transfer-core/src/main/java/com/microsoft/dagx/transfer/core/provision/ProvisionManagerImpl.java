package com.microsoft.dagx.transfer.core.provision;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Default provision manager. Invoke {@link #start(TransferProcessStore)} to initialize an instance.
 */
public class ProvisionManagerImpl implements ProvisionManager {
    private Vault vault;
    private TransferProcessStore processStore;
    private List<Provisioner<?, ?>> provisioners = new ArrayList<>();

    public ProvisionManagerImpl(Vault vault) {
        this.vault = vault;
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
        TransferProcess transferProcess = processStore.find(provisionedResource.getTransferProcessId());
        transferProcess.addProvisionedResource(provisionedResource);

        if (secretToken != null) {
            // TODO we should probably create a hierarchy for keys
            vault.storeSecret(provisionedResource.getId(), secretToken.getToken());
        }

        if (transferProcess.provisioningComplete()) {
            // TODO If all resources provisioned, delete scratch data
            transferProcess.transitionDeprovisioned();
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
