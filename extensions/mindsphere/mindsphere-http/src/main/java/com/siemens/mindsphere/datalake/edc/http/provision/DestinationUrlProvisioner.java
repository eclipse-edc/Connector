package com.siemens.mindsphere.datalake.edc.http.provision;

import com.siemens.mindsphere.datalake.edc.http.DataLakeClient;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.concurrent.CompletableFuture;

public class DestinationUrlProvisioner
        implements Provisioner<DestinationUrlResourceDefinition, DestinationUrlProvisionedResource> {
    public DestinationUrlProvisioner(DataLakeClient dataLakeClient, Monitor monitor, RetryPolicy<Object> retryPolicy) {
        this.dataLakeClient = dataLakeClient;
        this.monitor = monitor;
        this.retryPolicy = retryPolicy;
    }

    private final DataLakeClient dataLakeClient;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof DestinationUrlResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof DestinationUrlProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(DestinationUrlResourceDefinition resourceDefinition,
            Policy policy) {
        return DestinationUrlProvisionPipeline.Builder.newInstance(retryPolicy)
                .client(dataLakeClient)
                .monitor(monitor)
                .build()
                .provision(resourceDefinition)
                .thenApply(result -> provisionSuccedeed(resourceDefinition, result.getUrl()));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(DestinationUrlProvisionedResource provisionedResource,
            Policy policy) {
        // NOTE: there is nothing to de-provision

        return DestinationUrlProvisionPipeline.Builder.newInstance(retryPolicy)
                .client(dataLakeClient)
                .monitor(monitor)
                .build()
                .deprovision(provisionedResource)
                .thenApply(ignore -> StatusResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(provisionedResource.getId()).build()));
    }

    private StatusResult<ProvisionResponse> provisionSuccedeed(DestinationUrlResourceDefinition resourceDefinition, String url) {
        var resource = DestinationUrlProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getPath())
                .resourceDefinitionId(resourceDefinition.getId())
                .path(resourceDefinition.getPath())
                .url(url)
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .build();

        monitor.debug("DestinationUrlProvisioner: Url created for path: " + resourceDefinition.getPath());
        var response = ProvisionResponse.Builder.newInstance().resource(resource).build();
        return StatusResult.success(response);
    }
}
