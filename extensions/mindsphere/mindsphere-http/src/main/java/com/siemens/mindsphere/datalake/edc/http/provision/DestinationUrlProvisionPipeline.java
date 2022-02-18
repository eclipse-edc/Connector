package com.siemens.mindsphere.datalake.edc.http.provision;

import com.siemens.mindsphere.datalake.edc.http.DataLakeClient;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DestinationUrlProvisionPipeline {

    private DestinationUrlProvisionPipeline(RetryPolicy<Object> retryPolicy, Monitor monitor, DataLakeClient dataLakeClient) {
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.dataLakeClient = dataLakeClient;
    }

    private final RetryPolicy<Object> retryPolicy;
    private Monitor monitor;
    private DataLakeClient dataLakeClient;

    public CompletableFuture<DestinationUrlProvisionResponse> provision(DestinationUrlResourceDefinition resourceDefinition) {
        monitor.info("Provisioning destination HTTP url for path: " + resourceDefinition.getPath());
        return Failsafe.with(retryPolicy).getStageAsync(() -> CompletableFuture.supplyAsync(() -> dataLakeClient.getUploadUrl(resourceDefinition.getPath())).thenApply(url -> new DestinationUrlProvisionResponse(resourceDefinition.getPath(), url.toString())));
    }

    static class Builder {
        private final RetryPolicy<Object> retryPolicy;
        private Monitor monitor;
        private DataLakeClient dataLakeClient;

        private Builder(RetryPolicy<Object> retryPolicy) {
            this.retryPolicy = retryPolicy;
        }

        public static Builder newInstance(RetryPolicy<Object> policy) {
            return new Builder(policy);
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder client(DataLakeClient dataLakeClient) {
            this.dataLakeClient = dataLakeClient;
            return this;
        }

        public DestinationUrlProvisionPipeline build() {
            Objects.requireNonNull(retryPolicy);
            Objects.requireNonNull(dataLakeClient);
            Objects.requireNonNull(monitor);
            return new DestinationUrlProvisionPipeline(retryPolicy, monitor, dataLakeClient);
        }
    }
}
